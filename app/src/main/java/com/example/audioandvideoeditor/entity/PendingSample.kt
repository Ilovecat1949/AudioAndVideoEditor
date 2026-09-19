package com.example.audioandvideoeditor.entity

import android.media.MediaCodec

/**
 * Muxer 启动前暂存的编码 Sample 帧数据
 */
data class PendingSample(
    val trackIndex: Int,
    val data: ByteArray,
    val offset: Int,
    val size: Int,
    val presentationTimeUs: Long,
    val flags: Int
) {
    /**
     * 将暂存数据还原为 MediaCodec.BufferInfo
     */
    fun toBufferInfo(): MediaCodec.BufferInfo {
        return MediaCodec.BufferInfo().apply {
            set(offset, size, presentationTimeUs, flags)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PendingSample

        if (trackIndex != other.trackIndex) return false
        if (!data.contentEquals(other.data)) return false
        if (offset != other.offset) return false
        if (size != other.size) return false
        if (presentationTimeUs != other.presentationTimeUs) return false
        if (flags != other.flags) return false

        return true
    }

    override fun hashCode(): Int {
        var result = trackIndex
        result = 31 * result + data.contentHashCode()
        result = 31 * result + offset
        result = 31 * result + size
        result = 31 * result + presentationTimeUs.hashCode()
        result = 31 * result + flags
        return result
    }
}