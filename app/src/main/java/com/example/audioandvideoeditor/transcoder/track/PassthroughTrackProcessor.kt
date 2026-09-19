package com.example.audioandvideoeditor.transcoder.track

import android.media.MediaCodec
import android.media.MediaExtractor
import android.util.Log
import com.example.audioandvideoeditor.transcoder.infrastructure.MuxerWrapper
import java.nio.ByteBuffer

/**
 * 辅助轨道通用透传处理器
 */
class PassthroughTrackProcessor {

    private var muxerTrackIndex = -1
    private var isTrackAdded = false
    @Volatile
    private var isEOS = false

    private val bufferInfo = MediaCodec.BufferInfo()
    private val buffer = ByteBuffer.allocateDirect(1024 * 512) // 512KB 缓存

    /**
     * 单步搬运帧数据
     */
    fun processFrame(
        extractor: MediaExtractor,
        trackIndex: Int,
        muxerWrapper: MuxerWrapper
    ): Long {
        if (isEOS) return -1L

        try {
            if (!isTrackAdded) {
                val inputFormat = extractor.getTrackFormat(trackIndex)
                muxerTrackIndex = muxerWrapper.addTrack(inputFormat)
                isTrackAdded = true
            }

            buffer.clear()
            val sampleSize = extractor.readSampleData(buffer, 0)

            if (sampleSize < 0) {
                isEOS = true
                return -1L
            }

            val sampleTime = extractor.sampleTime
            val extractorFlags = extractor.sampleFlags

            var codecFlags = 0
            if ((extractorFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_KEY_FRAME
            }

            bufferInfo.set(0, sampleSize, sampleTime, codecFlags)

            // 只有当 muxer 准备就绪或能安全暂存时才写入
            muxerWrapper.writeSampleData(muxerTrackIndex, buffer, bufferInfo)

            if (!extractor.advance()) {
                isEOS = true
            }
            return sampleTime
        } catch (e: Exception) {
            Log.w("PassthroughProcessor", "Error processing passthrough track, marking as EOS", e)
            isEOS = true
            return -1L
        }
    }

    fun isDone(): Boolean = isEOS

    fun release() {
        buffer.clear()
    }
}