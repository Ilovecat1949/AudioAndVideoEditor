package com.example.audioandvideoeditor.transcoder.audio.filter

import android.util.Log
import com.example.audioandvideoeditor.model.AudioFormatParams
import java.nio.ByteBuffer

/**
 * 音频滤镜责任链（组合模式）
 */
class AudioFilterChain : IAudioFilter {
    private val filters = mutableListOf<IAudioFilter>()
    private var isPrepared = false

    fun addFilter(filter: IAudioFilter): AudioFilterChain {
        check(!isPrepared) { "Cannot add filter after AudioFilterChain is prepared!" }
        filters.add(filter)
        return this
    }

    override fun prepare(inputParams: AudioFormatParams, outputParams: AudioFormatParams) {
        var currentIn = inputParams
        // 简单处理：目前假设中间 Filter 保持输入输出参数一致，后续若有改变 Format 的 Filter 可在此动态传递
        for (filter in filters) {
            filter.prepare(currentIn, outputParams)
            currentIn = outputParams
        }
        isPrepared = true
    }

    override fun process(inputBuffer: ByteBuffer): ByteBuffer {
        if (filters.isEmpty() || !inputBuffer.hasRemaining()) {
            return inputBuffer
        }
        var currentBuffer = inputBuffer
        for (filter in filters) {
            currentBuffer = filter.process(currentBuffer)
        }
        return currentBuffer
    }

    override fun flush(): ByteBuffer? {
        // 逐级 flush 暂存的数据
        var flushedBuffer: ByteBuffer? = null
        for (filter in filters) {
            val res = filter.flush()
            if (res != null && res.hasRemaining()) {
                flushedBuffer = res // 此处简化处理，若有多级 flush，可后续叠加
            }
        }
        return flushedBuffer
    }

    override fun release() {
        for (filter in filters) {
            runCatching { filter.release() }
        }
        filters.clear()
        isPrepared = false
    }
}