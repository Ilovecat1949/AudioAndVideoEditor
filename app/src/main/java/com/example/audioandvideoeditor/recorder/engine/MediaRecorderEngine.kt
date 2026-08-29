package com.example.audioandvideoeditor.recorder.engine

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.Surface
import com.example.audioandvideoeditor.model.AudioSourceOption
import com.example.audioandvideoeditor.model.RecordingConfig

class MediaRecorderEngine(private val context: Context) : IRecorderEngine {

    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null

    override fun start(
        config: RecordingConfig,
        projection: MediaProjection,
        pfd: ParcelFileDescriptor
    ) {
        // API 31+ 推荐传入 Context 构造
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        this.mediaRecorder = recorder

        recorder.apply {
            if (config.audioOption == AudioSourceOption.MIC) {
                setAudioSource(MediaRecorder.AudioSource.MIC)
            }
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(pfd.fileDescriptor)

            setVideoSize(config.videoWidth, config.videoHeight)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)

            if (config.audioOption == AudioSourceOption.MIC) {
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(config.audioBitrate)
                setAudioSamplingRate(config.sampleRate)
            }

            setVideoEncodingBitRate(config.videoBitrate)
            setVideoFrameRate(config.frameRate)
            prepare()
        }

        val surface: Surface = recorder.surface
        virtualDisplay = projection.createVirtualDisplay(
            "MediaRecorderDisplay",
            config.videoWidth,
            config.videoHeight,
            config.dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface,
            null,
            null
        )

        recorder.start()
    }

    override fun pause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { mediaRecorder?.pause() }
        }
    }

    override fun resume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { mediaRecorder?.resume() }
        }
    }

    override fun stop() {
        // 录屏太短（如未满 1 秒）时 stop() 可能抛出 RuntimeException，做降级处理
        val success = runCatching { mediaRecorder?.stop() }.isSuccess
        if (!success) {
            Log.w("MediaRecorderEngine", "MediaRecorder stop 失败（可能录制时间过短）")
        }
        release()
    }

    override fun release() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null

            mediaRecorder?.reset()
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}