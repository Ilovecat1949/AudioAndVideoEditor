package com.example.audioandvideoeditor.recorder.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.audioandvideoeditor.model.AudioSourceOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

class AudioMixer(
    private val context: Context,
    private val mediaProjection: MediaProjection?,
    private val audioOption: AudioSourceOption,
    private val sampleRate: Int = 44100,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_STEREO,
    private val audioEncoding: Int = AudioFormat.ENCODING_PCM_16BIT
) {
    private var micAudioRecord: AudioRecord? = null
    private var internalAudioRecord: AudioRecord? = null

    private val isRecording = AtomicBoolean(false)
    private var workerJob: Job? = null

    // 在 AudioMixer 中保存对象引用
    private var echoCanceler: AcousticEchoCanceler? = null
    private val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding)

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope, onMixedDataAvailable: (ByteBuffer, Int) -> Unit) {
        if (isRecording.getAndSet(true)) return

        val bufferSizeShorts = minBufferSize / 2

        // 1. 初始化麦克风 AudioRecord
        if (audioOption == AudioSourceOption.MIC || audioOption == AudioSourceOption.MIXED) {
            // 🌟 动态选源：混合模式下使用 VOICE_COMMUNICATION 开启硬件 AEC 回声消除，仅麦克风模式使用 MIC 保持高保真音质
            val audioSource = if (audioOption == AudioSourceOption.MIXED) {
                MediaRecorder.AudioSource.VOICE_COMMUNICATION
            } else {
                MediaRecorder.AudioSource.MIC
            }
            micAudioRecord = AudioRecord(
                audioSource,
                sampleRate,
                channelConfig,
                audioEncoding,
                minBufferSize * 2
            )
//            这段代码的作用是显式向你的 AudioRecord 实例挂载 Android 系统原生的 AEC（Acoustic Echo Canceler，回声消除）数字信号处理器。
//            即使你将 AudioSource 设置为了 MIC（而非 VOICE_COMMUNICATION），只要设备硬件支持，这段代码也能强制开启硬件级的回声消除。
            // 开启时
// 🌟 1. 优化 AEC 初始化：安全链式调用，避免 !! 强转
            if (AcousticEchoCanceler.isAvailable() && audioOption == AudioSourceOption.MIXED) {
                micAudioRecord?.let { record ->
                    val sessionId = record.audioSessionId
                    if (sessionId != AudioRecord.ERROR_BAD_VALUE) {
                        echoCanceler = AcousticEchoCanceler.create(sessionId)?.apply {
                            enabled = true
                        }
                    }
                }
            }
        }

        // 2. 初始化系统内录 AudioRecord (仅支持 API 29+)
        if ((audioOption == AudioSourceOption.INTERNAL || audioOption == AudioSourceOption.MIXED) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaProjection != null
        ) {
            try {
                internalAudioRecord = createInternalAudioRecord(mediaProjection, minBufferSize * 2)
            } catch (e: Exception) {
                Log.e("AudioMixer", "创建系统内录失败: ${e.message}")
            }
        }

        // 3. 启动硬件录音
        micAudioRecord?.let { if (it.state == AudioRecord.STATE_INITIALIZED) it.startRecording() }
        internalAudioRecord?.let { if (it.state == AudioRecord.STATE_INITIALIZED) it.startRecording() }

        // 4. 开启同步采集与混音循环
        workerJob = scope.launch(Dispatchers.IO) {
            val micBuffer = ShortArray(bufferSizeShorts)
            val internalBuffer = ShortArray(bufferSizeShorts)
            val mixedBuffer = ShortArray(bufferSizeShorts)
            val byteBuffer = ByteBuffer.allocateDirect(minBufferSize).order(ByteOrder.nativeOrder())

// 🌟 1. 根据模式动态确定权重系数
            val (micVolume, internalVolume) = when (audioOption) {
                AudioSourceOption.MIXED -> 1.0f to 0.5f   // 混合模式：压低内录，突出人声
                AudioSourceOption.MIC -> 1.0f to 0.0f     // 纯麦克风：原声输出
                AudioSourceOption.INTERNAL -> 0.0f to 1.0f // 纯内录：无损无衰减输出
                AudioSourceOption.NONE -> 0.0f to 0.0f
            }
            while (isActive && isRecording.get()) {
                val micRecord = micAudioRecord
                val internalRecord = internalAudioRecord

                // 异步并行阻塞读取，确保两路硬件缓冲区保持步调一致
                val micDeferred = async {
                    micRecord?.read(micBuffer, 0, bufferSizeShorts, AudioRecord.READ_BLOCKING) ?: 0
                }
                val internalDeferred = async {
                    internalRecord?.read(internalBuffer, 0, bufferSizeShorts, AudioRecord.READ_BLOCKING) ?: 0
                }

                val micRead = micDeferred.await()
                val internalRead = internalDeferred.await()

                val validMic = if (micRead > 0) micRead else 0
                val validInternal = if (internalRead > 0) internalRead else 0
                val maxRead = maxOf(validMic, validInternal)

                if (maxRead > 0) {
                    for (i in 0 until maxRead) {
                        val micSample = if (i < validMic) micBuffer[i] * micVolume else 0f
                        val internalSample = if (i < validInternal) internalBuffer[i] * internalVolume else 0f

                        val mixed = (micSample + internalSample).toInt()
                        mixedBuffer[i] = mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }

                    byteBuffer.clear()
                    byteBuffer.asShortBuffer().put(mixedBuffer, 0, maxRead)
                    val bytesProcessed = maxRead * 2

                    onMixedDataAvailable(byteBuffer, bytesProcessed)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    private fun createInternalAudioRecord(projection: MediaProjection, bufferSize: Int): AudioRecord {
        val config = AudioPlaybackCaptureConfiguration.Builder(projection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()

        val format = AudioFormat.Builder()
            .setEncoding(audioEncoding)
            .setSampleRate(sampleRate)
            .setChannelMask(channelConfig)
            .build()

        return AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferSize)
            .setAudioPlaybackCaptureConfig(config)
            .build()
    }

    // 🌟 2. 优化后的 stop() 方法：先切断协程，后安全释放硬件
    fun stop() {
        if (!isRecording.getAndSet(false)) return

        // 第一步：必须先取消 Job，让 read() 循环能够顺利 break 退出
        workerJob?.cancel()
        workerJob = null

        echoCanceler?.apply {
            runCatching { enabled = false }
            runCatching { release() }
        }
        echoCanceler = null

        // 第二步：判断状态后安全 release
        try {
            micAudioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    runCatching { stop() }
                }
                runCatching { release() }
            }
            internalAudioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    runCatching { stop() }
                }
                runCatching { release() }
            }
        } catch (e: Exception) {
            Log.e("AudioMixer", "停止 AudioRecord 时捕获异常: ${e.message}")
        } finally {
            micAudioRecord = null
            internalAudioRecord = null
        }
    }
}