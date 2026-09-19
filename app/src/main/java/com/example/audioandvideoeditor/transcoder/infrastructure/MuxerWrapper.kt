package com.example.audioandvideoeditor.transcoder.infrastructure

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.example.audioandvideoeditor.entity.PendingSample
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 线程安全的 MediaMuxer 代理封装
 * 负责多轨同步注册、Buffer 暂存与 Flush，以及安全的生命周期控制
 */
class MuxerWrapper(
    private val muxer: MediaMuxer,
    private val expectedTrackCount: Int
) {

    @Volatile
    private var isStarted = false

    @Volatile
    private var hasWrittenAnySample = false

    private var addedTrackCount = 0
    private val pendingQueue = ConcurrentLinkedQueue<PendingSample>()

    /**
     * 注册轨道 Format
     * @return 对应的 Muxer Track Index
     */
    @Synchronized
    fun addTrack(format: MediaFormat): Int {
        if (isStarted) {
            throw IllegalStateException("Cannot add track after Muxer has started")
        }
        val trackIndex = muxer.addTrack(format)
        addedTrackCount++

        // 凑齐所有预期轨道后，触发真正启动并 Flush 暂存队列
        if (addedTrackCount >= expectedTrackCount) {
            muxer.start()
            isStarted = true
            flushPendingSamples()
        }
        return trackIndex
    }

    /**
     * 写入 Sample 数据（若 Muxer 未启动则自动深拷贝暂存）
     */
    fun writeSampleData(trackIndex: Int, byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (trackIndex < 0) return

        synchronized(this) {
            if (!isStarted) {
                // 1. 深拷贝 ByteBuffer
                val byteArray = ByteArray(bufferInfo.size)
                val originalPosition = byteBuffer.position()

                byteBuffer.position(bufferInfo.offset)
                byteBuffer.get(byteArray, 0, bufferInfo.size)
                byteBuffer.position(originalPosition) // 还原 position

                // 2. 入队暂存
                pendingQueue.add(
                    PendingSample(
                        trackIndex = trackIndex,
                        data = byteArray,
                        offset = 0,
                        size = bufferInfo.size,
                        presentationTimeUs = bufferInfo.presentationTimeUs,
                        flags = bufferInfo.flags
                    )
                )
                return
            }
        }

        // 3. 已启动，直接写入 Muxer
        doWrite(trackIndex, byteBuffer, bufferInfo)
    }

    private fun flushPendingSamples() {
        while (!pendingQueue.isEmpty()) {
            val sample = pendingQueue.poll() ?: break
            val buffer = ByteBuffer.wrap(sample.data)
            val info = sample.toBufferInfo()
            doWrite(sample.trackIndex, buffer, info)
        }
    }

    private fun doWrite(trackIndex: Int, byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (bufferInfo.size <= 0) return
        try {
            muxer.writeSampleData(trackIndex, byteBuffer, bufferInfo)
            hasWrittenAnySample = true
        } catch (e: Exception) {
            Log.e(TAG, "Error writing sample data to muxer", e)
            throw e
        }
    }

    /**
     * 安全停止并释放 Muxer（防止 Stop() Called but track is not started or stopped 崩溃）
     */
    fun safeRelease() {
        synchronized(this) {
            runCatching {
                if (isStarted && hasWrittenAnySample) {
                    muxer.stop()
                }
            }.onFailure { Log.e(TAG, "Failed to stop muxer safely", it) }

            runCatching {
                muxer.release()
            }.onFailure { Log.e(TAG, "Failed to release muxer", it) }

            isStarted = false
            pendingQueue.clear()
        }
    }

    fun isStarted(): Boolean = isStarted

    companion object {
        private const val TAG = "MuxerWrapper"
    }
}