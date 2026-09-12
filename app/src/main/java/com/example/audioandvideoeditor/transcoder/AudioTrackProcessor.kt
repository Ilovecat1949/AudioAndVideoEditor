package com.example.audioandvideoeditor.transcoder

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.example.audioandvideoeditor.model.AudioEncodeConfig
import java.nio.ByteBuffer

/**
 * 音频轨处理器
 * 支持旁路透传 (Passthrough) 与 重解码/重编码 (Re-encode) 两种策略
 */
class AudioTrackProcessor(
    private val config: AudioEncodeConfig
) {

    private var decoder: MediaCodec? = null
    private var encoder: MediaCodec? = null

    private var muxerTrackIndex = -1
    private var isMuxerStarted = false

    private val bufferInfo = MediaCodec.BufferInfo()
    private val passthroughBuffer = ByteBuffer.allocateDirect(1024 * 512) // 512KB 缓冲区

    private var isExtractorEOS = false
    private var isDecoderEOS = false
    private var isEncoderEOS = false

    /**
     * 初始化音频处理组件
     */
    fun prepare(
        inputFormat: MediaFormat
    ) {
        if (config.isPassthrough) {
            // Passthrough 模式下无需创建编解码器
            return
        }

        // 1. 创建音频编码器
        val outputFormat = MediaFormat.createAudioFormat(
            config.mimeType,
            config.sampleRate,
            config.channelCount
        ).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, config.bitrate)
            setInteger(MediaFormat.KEY_AAC_PROFILE, android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        }

        encoder = MediaCodec.createEncoderByType(config.mimeType).apply {
            configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            start()
        }

        // 2. 创建音频解码器
        val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: throw IllegalArgumentException("Invalid audio MIME type")
        decoder = MediaCodec.createDecoderByType(mime).apply {
            configure(inputFormat, null, null, 0)
            start()
        }
    }

    /**
     * 单步音频帧处理（由后台线程循环调度）
     * @return 当前处理的音频 PTS (微秒)；处理完毕返回 -1L
     */
    fun processFrame(
        extractor: MediaExtractor,
        trackIndex: Int,
        muxer: MediaMuxer,
        onMuxerStartCheck: (MediaFormat) -> Int
    ): Long {
        return if (config.isPassthrough) {
            processPassthroughFrame(extractor, trackIndex, muxer, onMuxerStartCheck)
        } else {
            processReEncodeFrame(extractor, trackIndex, muxer, onMuxerStartCheck)
        }
    }

    /**
     * 极速 Passthrough 处理通道
     */
    private fun processPassthroughFrame(
        extractor: MediaExtractor,
        trackIndex: Int,
        muxer: MediaMuxer,
        onMuxerStartCheck: (MediaFormat) -> Int
    ): Long {
        if (isExtractorEOS) return -1L

        if (!isMuxerStarted) {
            val inputFormat = extractor.getTrackFormat(trackIndex)
            muxerTrackIndex = onMuxerStartCheck(inputFormat)
            isMuxerStarted = true
        }

        passthroughBuffer.clear()
        val sampleSize = extractor.readSampleData(passthroughBuffer, 0)

        if (sampleSize < 0) {
            isExtractorEOS = true
            return -1L
        }

        val sampleTime = extractor.sampleTime
        val extractorFlags = extractor.sampleFlags

        // 🌟 核心修正：将 MediaExtractor 的 flags 显式转换为 MediaCodec 的 buffer flags
        var codecFlags = 0
        if ((extractorFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
            codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_KEY_FRAME
        }
        if ((extractorFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
            codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
        }

        bufferInfo.set(0, sampleSize, sampleTime, codecFlags)
        muxer.writeSampleData(muxerTrackIndex, passthroughBuffer, bufferInfo)

        extractor.advance()
        return sampleTime
    }

    /**
     * Re-encode 重新编解码处理通道
     */
    private fun processReEncodeFrame(
        extractor: MediaExtractor,
        trackIndex: Int,
        muxer: MediaMuxer,
        onMuxerStartCheck: (MediaFormat) -> Int
    ): Long {
        if (isEncoderEOS) return -1L

        var currentPresentationTimeUs = 0L

        // Step 1: Extractor -> Decoder
        if (!isExtractorEOS) {
            val inputIndex = decoder!!.dequeueInputBuffer(TIMEOUT_US)
            if (inputIndex >= 0) {
                val inputBuffer = decoder!!.getInputBuffer(inputIndex)
                if (inputBuffer != null) {
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        decoder!!.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isExtractorEOS = true
                    } else {
                        val sampleTime = extractor.sampleTime
                        decoder!!.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
        }

        // Step 2: Decoder (PCM) -> Encoder
        if (!isDecoderEOS) {
            val decoderStatus = decoder!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            if (decoderStatus >= 0) {
                val pcmBuffer = decoder!!.getOutputBuffer(decoderStatus)
                if (pcmBuffer != null && bufferInfo.size > 0) {
                    val encoderInputIndex = encoder!!.dequeueInputBuffer(TIMEOUT_US)
                    if (encoderInputIndex >= 0) {
                        val encoderInputBuffer = encoder!!.getInputBuffer(encoderInputIndex)
                        if (encoderInputBuffer != null) {
                            encoderInputBuffer.clear()
                            encoderInputBuffer.put(pcmBuffer)
                            encoder!!.queueInputBuffer(
                                encoderInputIndex,
                                0,
                                bufferInfo.size,
                                bufferInfo.presentationTimeUs,
                                bufferInfo.flags
                            )
                        }
                    }
                }
                decoder!!.releaseOutputBuffer(decoderStatus, false)

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    isDecoderEOS = true
                    // 标记编码器结束
                    val encoderInputIndex = encoder!!.dequeueInputBuffer(TIMEOUT_US)
                    if (encoderInputIndex >= 0) {
                        encoder!!.queueInputBuffer(encoderInputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    }
                }
            }
        }

        // Step 3: Encoder -> Muxer
        while (true) {
            val encoderStatus = encoder!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (isMuxerStarted) {
                    throw RuntimeException("Audio muxer format changed twice")
                }
                val newFormat = encoder!!.outputFormat
                muxerTrackIndex = onMuxerStartCheck(newFormat)
                isMuxerStarted = true
            } else if (encoderStatus >= 0) {
                val encodedData = encoder!!.getOutputBuffer(encoderStatus)
                    ?: throw RuntimeException("Audio encoderOutputBuffer $encoderStatus was null")

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0
                }

                if (bufferInfo.size != 0 && isMuxerStarted) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(muxerTrackIndex, encodedData, bufferInfo)
                    currentPresentationTimeUs = bufferInfo.presentationTimeUs
                }

                encoder!!.releaseOutputBuffer(encoderStatus, false)

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    isEncoderEOS = true
                    break
                }
            }
        }

        return currentPresentationTimeUs
    }

    fun release() {
        runCatching { decoder?.stop(); decoder?.release() }
        runCatching { encoder?.stop(); encoder?.release() }
        decoder = null
        encoder = null
    }

    companion object {
        private const val TIMEOUT_US = 2500L
    }
}