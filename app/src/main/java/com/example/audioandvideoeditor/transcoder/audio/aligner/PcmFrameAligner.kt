package com.example.audioandvideoeditor.transcoder.audio.aligner

import com.example.audioandvideoeditor.model.AudioFormatParams
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * PCM 帧对齐与精确 PTS 算子
 *
 * 职责：
 * 1. 积攒从 Filter Chain 输出的任意尺寸 PCM 数据。
 * 2. 严格按 AAC 标准帧（1024 Samples * Channels * 2 Bytes）切片为 4096 字节的完整 Chunk。
 * 3. 基于实际处理输出的 Sample 数量计算绝对精确的递增 PTS，彻底消除重采样/变速后的音视频不同步和爆音。
 */
class PcmFrameAligner(
    private val targetFormat: AudioFormatParams
) {
    /** AAC 硬件编码器标准帧 Sample 数量 (1024) */
    private val samplesPerFrame = 1024

    /** 目标单帧精细字节数：1024 * 通道数 * 2 (例如双声道 16bit 为 4096 字节) */
    val targetBytesPerFrame: Int = samplesPerFrame * targetFormat.frameSize

    private val accumulator = ByteArrayOutputStream()

    /** 记录起始时间基准 (Us) */
    private var startPtsUs: Long = -1L

    /** 已经输出的总 Sample 数量 */
    private var totalOutputSamples: Long = 0L

    /**
     * 攒包并切片输出固定字节数的 PCM 帧（直接回调 ByteBuffer）
     *
     * @param inputBuffer Filter 加工后的 PCM 数据
     * @param currentInputPtsUs 解码器/上游当前帧的 PTS (Us)
     * @param onFrameReady 切片对齐后的回调，提供 readyBuffer (ByteBuffer) 与精准算出的 framePtsUs
     */
    fun pushAndAlign(
        inputBuffer: ByteBuffer,
        currentInputPtsUs: Long,
        onFrameReady: (readyBuffer: ByteBuffer, framePtsUs: Long) -> Unit
    ) {
        // 记录首帧 PTS 作为起始基准时间
        if (startPtsUs == -1L && currentInputPtsUs >= 0) {
            startPtsUs = currentInputPtsUs
        }

        val bytesToRead = inputBuffer.remaining()
        if (bytesToRead <= 0) return

        val tempArray = ByteArray(bytesToRead)
        inputBuffer.get(tempArray)
        accumulator.write(tempArray)

        val bufferArray = accumulator.toByteArray()
        var offset = 0
        var remainingBytes = bufferArray.size

        // 只要积攒的数据大于等于一帧标准大小（如 4096 字节），就切片输出
        while (remainingBytes >= targetBytesPerFrame) {
            val chunk = ByteArray(targetBytesPerFrame)
            System.arraycopy(bufferArray, offset, chunk, 0, targetBytesPerFrame)

            // 基于输出的总 Sample 数精算当前帧 PTS
            val framePtsUs = calculateNextPtsUs()

            // 递增 Sample 计数器
            totalOutputSamples += samplesPerFrame

            // 使用 ByteBuffer.wrap 直接输出，零额外数组拷贝
            onFrameReady(ByteBuffer.wrap(chunk), framePtsUs)

            offset += targetBytesPerFrame
            remainingBytes -= targetBytesPerFrame
        }

        // 清空已消费的数据，保留未凑满一帧的尾部余量
        accumulator.reset()
        if (remainingBytes > 0) {
            accumulator.write(bufferArray, offset, remainingBytes)
        }
    }

    /**
     * 流结束 (EOS) 时，刷空尾部不足 4096 字节的数据（补 0 静音对齐）
     */
    fun flushRemaining(onFrameReady: (readyBuffer: ByteBuffer, framePtsUs: Long) -> Unit) {
        val remainingBytes = accumulator.size()
        if (remainingBytes > 0) {
            val chunk = ByteArray(targetBytesPerFrame) // 默认全填充 0 (PCM 静音)
            val bufferArray = accumulator.toByteArray()

            // 拷贝剩余数据，未填满的后续字节自动维持静音 0
            System.arraycopy(bufferArray, 0, chunk, 0, remainingBytes)

            val framePtsUs = calculateNextPtsUs()
            totalOutputSamples += samplesPerFrame

            onFrameReady(ByteBuffer.wrap(chunk), framePtsUs)
            accumulator.reset()
        }
    }

    /**
     * 重置算子状态
     */
    fun reset() {
        accumulator.reset()
        startPtsUs = -1L
        totalOutputSamples = 0L
    }

    /**
     * 精确 PTS 递增计算核心算子
     * 公式: PTS = startPtsUs + (totalOutputSamples * 1,000,000) / targetSampleRate
     */
    private fun calculateNextPtsUs(): Long {
        val base = if (startPtsUs >= 0) startPtsUs else 0L
        return base + (totalOutputSamples * 1_000_000L) / targetFormat.sampleRate
    }
}