package com.example.audioandvideoeditor.transcoder.audio.bridge

import com.example.audioandvideoeditor.model.AudioFilterType
import java.nio.ByteBuffer

/**
 * 统一的 Native 音频滤镜桥接类
 */
class FFmpegAudioFilterBridge(
    private val filterType: AudioFilterType // 强类型约束，禁止传任意 Int
) {
    private var nativeHandle: Long = 0L

    fun init(inSampleRate: Int, inChannels: Int, outSampleRate: Int, outChannels: Int): Boolean {
        // 传递 filterType.nativeId 给 C++ 层
        nativeHandle = nativeInit(filterType.nativeId, inSampleRate, inChannels, outSampleRate, outChannels)
        return nativeHandle != 0L
    }

    fun process(inputBuffer: ByteBuffer, inputSize: Int): ByteBuffer? {
        if (nativeHandle == 0L || !inputBuffer.isDirect) return null
        return nativeProcess(nativeHandle, inputBuffer, inputSize)
    }

    fun flush(): ByteBuffer? {
        if (nativeHandle == 0L) return null
        return nativeFlush(nativeHandle)
    }

    fun release() {
        if (nativeHandle != 0L) {
            nativeRelease(nativeHandle)
            nativeHandle = 0L
        }
    }

    private external fun nativeInit(type: Int, inSR: Int, inCh: Int, outSR: Int, outCh: Int): Long
    private external fun nativeProcess(handle: Long, inBuffer: ByteBuffer, inSize: Int): ByteBuffer?
    private external fun nativeFlush(handle: Long): ByteBuffer?
    private external fun nativeRelease(handle: Long)

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }
}