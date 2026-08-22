package com.example.audioandvideoeditor.recorder.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
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

    private val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding)

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope, onMixedDataAvailable: (ByteBuffer, Int) -> Unit) {
        if (isRecording.getAndSet(true)) return

        val bufferSizeShorts = minBufferSize / 2

        // 1. 初始化麦克风 AudioRecord
        if (audioOption == AudioSourceOption.MIC || audioOption == AudioSourceOption.MIXED) {
            micAudioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioEncoding,
                minBufferSize * 2
            )
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
                    // 带增益系数的抗爆音混音算法 (各取 0.75 增益平滑叠加)
                    for (i in 0 until maxRead) {
                        val micSample = if (i < validMic) micBuffer[i] * 0.75f else 0f
                        val internalSample = if (i < validInternal) internalBuffer[i] * 0.75f else 0f

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

    fun stop() {
        if (!isRecording.getAndSet(false)) return

        workerJob?.cancel()
        workerJob = null

        try {
            micAudioRecord?.apply {
                if (state == AudioRecord.STATE_INITIALIZED) stop()
                release()
            }
            internalAudioRecord?.apply {
                if (state == AudioRecord.STATE_INITIALIZED) stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            micAudioRecord = null
            internalAudioRecord = null
        }
    }
}