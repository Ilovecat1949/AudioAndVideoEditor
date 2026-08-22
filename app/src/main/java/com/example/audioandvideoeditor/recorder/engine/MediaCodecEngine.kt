package com.example.audioandvideoeditor.recorder.engine

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.os.Build
import android.os.ParcelFileDescriptor
import android.view.Surface
import com.example.audioandvideoeditor.model.AudioSourceOption
import com.example.audioandvideoeditor.model.RecordingConfig
import com.example.audioandvideoeditor.recorder.audio.AudioMixer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class MediaCodecEngine(private val context: Context) : IRecorderEngine {

    private var mediaMuxer: MediaMuxer? = null
    private var videoCodec: MediaCodec? = null
    private var audioCodec: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var audioMixer: AudioMixer? = null

    private var videoTrackIndex = -1
    private var audioTrackIndex = -1

    @Volatile
    private var isMuxerStarted = false

    private val isRecording = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)

    private var engineScope: CoroutineScope? = null
    private var audioDrainJob: Job? = null
    private var videoDrainJob: Job? = null

    private var audioTotalSamplesRead = 0L

    private var m_pfd: ParcelFileDescriptor?=null

    // 声明临时文件变量
    private var tempAudioVideoFile: File? = null
    override fun start(
        config: RecordingConfig,
        projection: MediaProjection,
        pfd: ParcelFileDescriptor
    ) {
        if (isRecording.getAndSet(true)) return
        m_pfd=pfd
        engineScope = CoroutineScope(Dispatchers.IO)

        // 1. 初始化 MediaMuxer
        // 版本分支适配 API 24/25
        mediaMuxer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            MediaMuxer(pfd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } else {
            val tempFile = File(context.cacheDir, "recording_tmp_${System.currentTimeMillis()}.mp4")
            tempAudioVideoFile = tempFile
            MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        }

        // 2. 配置与启动视频 MediaCodec (Surface 输入)
        setupVideoCodec(config)

        // 3. 配置与启动 VirtualDisplay
        inputSurface?.let { surface ->
            virtualDisplay = projection.createVirtualDisplay(
                "CodecRecordingDisplay",
                config.videoWidth,
                config.videoHeight,
                config.dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null
            )
        }

        val hasAudio = config.audioOption != AudioSourceOption.NONE

        // 4. 配置与启动音频 MediaCodec (若开启音频)
        if (hasAudio) {
            setupAudioCodec(config)
        }

        videoCodec?.start()
        audioCodec?.start()

        // 5. 开启音视频编码数据读取循环
        startDrainingVideo()

        if (hasAudio) {
            startDrainingAudio()
            // 启动 AudioMixer 采集双路 PCM 并写入 audioCodec
            audioMixer = AudioMixer(
                context = context,
                mediaProjection = projection,
                audioOption = config.audioOption,
                sampleRate = config.sampleRate
            ).apply {
                start(engineScope!!) { pcmData, size ->
                    if (!isPaused.get()) {
                        feedAudioDataToCodec(pcmData, size, config.sampleRate)
                    }
                }
            }
        }
    }

    private fun setupVideoCodec(config: RecordingConfig) {
        val format = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            config.videoWidth,
            config.videoHeight
        ).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, config.videoBitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, config.frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1秒一次关键帧
        }

        videoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = createInputSurface()
        }
    }

    private fun setupAudioCodec(config: RecordingConfig) {
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            config.sampleRate,
            2 // 双声道
        ).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, config.audioBitrate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }

        audioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }
    }

    private fun feedAudioDataToCodec(pcmData: ByteBuffer, size: Int, sampleRate: Int) {
        val codec = audioCodec ?: return
        val inputBufferIndex = codec.dequeueInputBuffer(10_000L)
        if (inputBufferIndex >= 0) {
            val inputBuffer = codec.getInputBuffer(inputBufferIndex) ?: return
            inputBuffer.clear()
            inputBuffer.put(pcmData)

            // PTS 根据音频采样点数推算，保证绝对精确同步
            val ptsUs = audioTotalSamplesRead * 1_000_000L / sampleRate / 2 // 16-bit 双声道，单采样点占 4 字节
            audioTotalSamplesRead += size / 2

            codec.queueInputBuffer(inputBufferIndex, 0, size, ptsUs, 0)
        }
    }

    private fun startDrainingVideo() {
        videoDrainJob = engineScope?.launch {
            val codec = videoCodec ?: return@launch
            val bufferInfo = MediaCodec.BufferInfo()
            var firstVideoPtsUs = -1L // 🌟 1. 声明首帧视频时间戳基准

            while (isActive && isRecording.get()) {
                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
                when (outputBufferIndex) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        synchronized(this@MediaCodecEngine) {
                            if (videoTrackIndex == -1) {
                                videoTrackIndex = mediaMuxer?.addTrack(codec.outputFormat) ?: -1
                                checkAndStartMuxer()
                            }
                        }
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> continue
                    else -> {
                        if (outputBufferIndex >= 0) {
                            val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                            if (outputBuffer != null && bufferInfo.size > 0 && isMuxerStarted) {
                                if (!isPaused.get() && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {

                                    // 🌟 2. 扣除开机时间偏移，强制让视频时间戳从 0 开始
                                    if (firstVideoPtsUs == -1L) {
                                        firstVideoPtsUs = bufferInfo.presentationTimeUs
                                    }
                                    bufferInfo.presentationTimeUs = (bufferInfo.presentationTimeUs - firstVideoPtsUs).coerceAtLeast(0L)

                                    mediaMuxer?.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                                }
                            }
                            codec.releaseOutputBuffer(outputBufferIndex, false)
                        }
                    }
                }
            }
        }
    }

    private fun startDrainingAudio() {
        audioDrainJob = engineScope?.launch {
            val codec = audioCodec ?: return@launch
            val bufferInfo = MediaCodec.BufferInfo()

            while (isActive && isRecording.get()) {
                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
                when (outputBufferIndex) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        synchronized(this@MediaCodecEngine) {
                            if (audioTrackIndex == -1) {
                                audioTrackIndex = mediaMuxer?.addTrack(codec.outputFormat) ?: -1
                                checkAndStartMuxer()
                            }
                        }
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> continue
                    else -> {
                        if (outputBufferIndex >= 0) {
                            val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                            if (outputBuffer != null && bufferInfo.size > 0 && isMuxerStarted) {
                                if (!isPaused.get() && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                    mediaMuxer?.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo)
                                }
                            }
                            codec.releaseOutputBuffer(outputBufferIndex, false)
                        }
                    }
                }
            }
        }
    }

    @Synchronized
    private fun checkAndStartMuxer() {
        if (isMuxerStarted) return
        val hasAudioTrack = audioCodec != null
        val videoReady = videoTrackIndex >= 0
        val audioReady = !hasAudioTrack || audioTrackIndex >= 0

        if (videoReady && audioReady) {
            mediaMuxer?.start()
            isMuxerStarted = true
        }
    }

    override fun pause() {
        isPaused.set(true)
    }

    override fun resume() {
        isPaused.set(false)
    }

    override fun stop() {
        if (!isRecording.getAndSet(false)) return

        audioMixer?.stop()
        audioMixer = null

        engineScope?.cancel()
        engineScope = null

        release()
    }

    override fun release() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null

            videoCodec?.apply {
                stop()
                release()
            }
            videoCodec = null

            audioCodec?.apply {
                stop()
                release()
            }
            audioCodec = null

            inputSurface?.release()
            inputSurface = null

            if (isMuxerStarted) {
                mediaMuxer?.apply {
                    stop()
                    release()
                }
            }
            mediaMuxer = null
            isMuxerStarted = false
            videoTrackIndex = -1
            audioTrackIndex = -1
            audioTotalSamplesRead = 0L
            // API 24/25 兼容处理：将 cache 目录的临时文件写入目标 MediaStore FileDescriptor
            tempAudioVideoFile?.let { tempFile ->
                if (tempFile.exists()) {
                    try {
                        FileInputStream(tempFile).use { input ->
                            FileOutputStream(m_pfd?.fileDescriptor).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    tempFile.delete()
                }
                tempAudioVideoFile = null
            }
            m_pfd = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}