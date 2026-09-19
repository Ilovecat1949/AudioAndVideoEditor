package com.example.audioandvideoeditor.transcoder.audio.filter

import com.example.audioandvideoeditor.model.AudioFormatParams
import java.nio.ByteBuffer

/**
 * 音频滤镜统一接口
 */
interface IAudioFilter {
    /**
     * 初始化/准备滤镜
     */
    fun prepare(inputParams: AudioFormatParams, outputParams: AudioFormatParams)

    /**
     * 处理 PCM 缓冲区数据
     * @param inputBuffer 必须为 DirectByteBuffer，确保 Native 交互零拷贝
     * @return 处理后的 DirectByteBuffer（如果未做改动，可直接返回 inputBuffer）
     */
    fun process(inputBuffer: ByteBuffer): ByteBuffer

    /**
     * 刷空滤镜内部残留的数据（EOS 阶段调用）
     * @return 残留数据构成的 ByteBuffer，若无残留则返回 null
     */
    fun flush(): ByteBuffer?

    /**
     * 释放 Native/内存 资源
     */
    fun release()
}