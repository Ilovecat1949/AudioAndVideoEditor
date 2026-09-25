package com.example.audioandvideoeditor.transcoder.audio.aligner

import com.example.audioandvideoeditor.model.AudioFormatParams
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * PCM 帧对齐与精确 PTS 算子（支持非连续音频 Timeline 自动补 0 填充静音）
 *
 * 职责：
 * 1. 积攒从 Filter Chain 输出的任意尺寸 PCM 数据。
 * 2. 自动检测输入时间戳断层（Gap），填充静音帧平滑对齐时间轴。
 * 3. 严格按 AAC 标准帧（1024 Samples * Channels * 2 Bytes）切片为 4096 字节的完整 Chunk。
 * 4. 基于实际处理输出的 Sample 数量计算绝对精确的递增 PTS。
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

    /** 容忍的时间戳偏差阈值（微秒）：超过半帧 (10ms) 认为存在时间断层 */
    private val gapThresholdUs = (1000000L * samplesPerFrame / targetFormat.sampleRate) / 2

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
        val bytesToRead = inputBuffer.remaining()
        if (bytesToRead <= 0) return

        // 1. 初始化起始时间基准
        if (startPtsUs == -1L && currentInputPtsUs >= 0) {
            startPtsUs = currentInputPtsUs
        }

        // 2. 时间断层（Gap）检测与静音帧 Padding 逻辑
        if (startPtsUs >= 0 && currentInputPtsUs > 0) {
            val expectedPtsUs = calculateNextPtsUs()
            val timeGapUs = currentInputPtsUs - expectedPtsUs

            // 如果当前输入 PTS 明显落后于预期的下一个 PTS，说明中间有音频缺失断层
            if (timeGapUs > gapThresholdUs) {
                fillSilenceGap(timeGapUs, onFrameReady)
            }
        }

        // 3. 将当前实际数据写入 accumulator
        val tempArray = ByteArray(bytesToRead)
        inputBuffer.get(tempArray)
        accumulator.write(tempArray)

        // 4. 切片输出凑满 4096 字节的完整 Chunk
        drainAccumulator(onFrameReady)
    }

    /**
     * 向管道中自动填充静音数据以弥补时间隙 (Gap)
     */
    private fun fillSilenceGap(
        gapDurationUs: Long,
        onFrameReady: (readyBuffer: ByteBuffer, framePtsUs: Long) -> Unit
    ) {
        // 根据断层时长计算缺失的 Sample 数量
        val missingSamples = (gapDurationUs * targetFormat.sampleRate) / 1_000_000L
        // 计算对应的静音 PCM 字节数 (Samples * frameSize)
        val missingBytes = (missingSamples * targetFormat.frameSize).toInt()

        if (missingBytes > 0) {
            // 创建全 0 的静音 ByteArray
            val silenceBuffer = ByteArray(missingBytes)
            accumulator.write(silenceBuffer)

            // 优先刷出静音帧，把时间轴垫平
            drainAccumulator(onFrameReady)
        }
    }

    /**
     * 只要积攒的数据大于等于一帧标准大小（4096 字节），就切片回调输出
     */
    private fun drainAccumulator(
        onFrameReady: (readyBuffer: ByteBuffer, framePtsUs: Long) -> Unit
    ) {
        val bufferArray = accumulator.toByteArray()
        var offset = 0
        var remainingBytes = bufferArray.size

        while (remainingBytes >= targetBytesPerFrame) {
            val chunk = ByteArray(targetBytesPerFrame)
            System.arraycopy(bufferArray, offset, chunk, 0, targetBytesPerFrame)

            // 基于输出的总 Sample 数精算当前帧 PTS
            val framePtsUs = calculateNextPtsUs()

            // 递增 Sample 计数器
            totalOutputSamples += samplesPerFrame

            // 使用 ByteBuffer.wrap 输出，零额外数组拷贝
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
            val chunk = ByteArray(targetBytesPerFrame) // 默认全 0 (PCM 静音)
            val bufferArray = accumulator.toByteArray()

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