package com.example.audioandvideoeditor.transcoder.audio.filter

import android.util.Log
import com.example.audioandvideoeditor.model.AudioFilterType
import com.example.audioandvideoeditor.model.AudioFormatParams
import com.example.audioandvideoeditor.transcoder.audio.bridge.FFmpegAudioFilterBridge
import java.nio.ByteBuffer

/**
 * 音频重采样 & 声道转换滤镜
 *
 * 职责：
 * 1. 检查输入/输出采样率与声道数。若完全一致则智能 Bypass（零拷贝透传）。
 * 2. 若存在差异，调用 Native 层的 FFmpeg Swresample 进行高质量采样率转换与声道映射。
 */
class SwresampleAudioFilter : IAudioFilter {

    val type: AudioFilterType = AudioFilterType.RESAMPLE

    private var nativeBridge: FFmpegAudioFilterBridge? = null
    private var isNeedResample: Boolean = false

    override fun prepare(inputParams: AudioFormatParams, outputParams: AudioFormatParams) {
        // 判断是否需要做重采样/声道转换
        isNeedResample = inputParams.sampleRate != outputParams.sampleRate ||
                inputParams.channelCount != outputParams.channelCount
        if (isNeedResample) {
            // 释放旧的桥接实例（若有）
//            release()

            nativeBridge = FFmpegAudioFilterBridge(AudioFilterType.RESAMPLE).apply {
                init(
                    inSampleRate = inputParams.sampleRate,
                    inChannels = inputParams.channelCount,
                    inSampleFmt=inputParams.ffmpegSampleFmt,
                    outSampleRate = outputParams.sampleRate,
                    outChannels = outputParams.channelCount,
                    outSampleFmt = outputParams.ffmpegSampleFmt
                )
            }
        }
    }

    override fun process(inputBuffer: ByteBuffer): ByteBuffer {
        // 如果不需要重采样或输入为空，直接透传返回
        if (!isNeedResample || nativeBridge == null) {
            return inputBuffer
        }

        val inputSize = inputBuffer.remaining()
        if (inputSize <= 0) {
            Log.d("nativeBridge?.process","inputSize <= 0")
            return inputBuffer
        }

        // 必须为 DirectByteBuffer 才能通过 JNI 实现 C++ 零拷贝访问
        if (!inputBuffer.isDirect) {
            return inputBuffer
        }
        Log.d("nativeBridge?.process","hhhhhhhhh")
        // 调用 JNI 执行 C++ 重采样
        val resampledBuffer = nativeBridge?.process(inputBuffer, inputSize)

        // 若 Native 处理失败则降级透传原 Buffer，保障管线不崩溃
        return resampledBuffer ?: inputBuffer
    }

    override fun flush(): ByteBuffer? {
        if (!isNeedResample) return null
        return nativeBridge?.flush()
    }

    override fun release() {
        nativeBridge?.release()
        nativeBridge = null
        isNeedResample = false
    }
}