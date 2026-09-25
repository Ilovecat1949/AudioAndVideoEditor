package com.example.audioandvideoeditor.transcoder.track

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.example.audioandvideoeditor.model.AudioEncodeConfig
import com.example.audioandvideoeditor.transcoder.audio.aligner.PcmFrameAligner
import com.example.audioandvideoeditor.transcoder.audio.filter.AudioFilterChain
import com.example.audioandvideoeditor.transcoder.audio.filter.SwresampleAudioFilter
import com.example.audioandvideoeditor.model.AudioFormatParams
import com.example.audioandvideoeditor.transcoder.infrastructure.MuxerWrapper
import java.nio.ByteBuffer

/**
 * 重构后的音频轨硬件转码处理器（支持 Pipe-Filter 责任链 + 精确切片 PTS 算子）
 *
 * 架构链路：
 * Extractor -> Decoder -> AudioFilterChain (Resample/Atempo/Volume) -> PcmFrameAligner (4096 bytes / PTS) -> Encoder -> Muxer
 */
class AudioTrackProcessor(
    private val config: AudioEncodeConfig,
    private val filterChain: AudioFilterChain = AudioFilterChain()
) {

    private var decoder: MediaCodec? = null
    private var encoder: MediaCodec? = null

    private var muxerTrackIndex = -1
    private var isTrackAdded = false

    private val bufferInfo = MediaCodec.BufferInfo()
    private var passthroughBuffer: ByteBuffer? = null

    // 管线核心组件
    private var frameAligner: PcmFrameAligner? = null
    private var inputAudioFormat: AudioFormatParams? = null
    private var outputAudioFormat: AudioFormatParams? = null

    // 状态控制标志位
    private var isExtractorEOS = false
    private var isDecoderEOS = false
    private var isEncoderEOS = false

    private var audioRetryCount = 0

    fun prepare(inputFormat: MediaFormat) {
        if (config.isPassthrough) {
            passthroughBuffer = ByteBuffer.allocateDirect(1024 * 512) // 512KB 透传缓冲区
            return
        }

        // 1. 解析与初始化音频参数模型
        val inSampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val inChannelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val pcmEncoding = if (inputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            inputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
        } else {
            // 兼容性兜底：较旧设备或部分解码器可能不包含此 Key，默认按标准的 16-bit PCM 处理
            AudioFormat.ENCODING_PCM_16BIT
        }


        inputAudioFormat = AudioFormatParams(sampleRate = inSampleRate, channelCount = inChannelCount, pcmEncoding = pcmEncoding)
        outputAudioFormat = AudioFormatParams(sampleRate = config.sampleRate, channelCount =config.channelCount,pcmEncoding=config.pcmEncoding)

         // 1. 判断是否真的需要重采样/声道转换
        val needResample = inSampleRate != config.sampleRate ||
                inChannelCount != config.channelCount
        if (needResample) {
            // 仅在格式不匹配时，才将 SwresampleAudioFilter 压入第 0 位
            // 由于 AudioFilterChain 未 prepare，直接追加重采样滤镜
            // 保证重采样/声道转换作为管线的第一道工序处理 Raw PCM
            filterChain.addFilter(SwresampleAudioFilter())
        }
        filterChain.prepare(inputAudioFormat!!, outputAudioFormat!!)

        // 3. 初始化帧切片与精确 PTS 算子
        frameAligner = PcmFrameAligner(outputAudioFormat!!)

        // 4. 创建音频编码器 (AAC Object LC)
        val outputFormat = MediaFormat.createAudioFormat(
            config.mimeType,
            config.sampleRate,
            config.channelCount
        ).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, config.bitrate)
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        }

        encoder = MediaCodec.createEncoderByType(config.mimeType).apply {
            configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            start()
        }

        // 5. 创建音频解码器，使用 Clean Format 清理非标元数据
        val mime = inputFormat.getString(MediaFormat.KEY_MIME)
            ?: throw IllegalArgumentException("Invalid audio MIME type")

        val cleanInputFormat = MediaFormat.createAudioFormat(
            mime,
            inSampleRate,
            inChannelCount
        ).apply {
            if (inputFormat.containsKey("csd-0")) {
                setByteBuffer("csd-0", inputFormat.getByteBuffer("csd-0"))
            }
        }

        decoder = MediaCodec.createDecoderByType(mime).apply {
            configure(cleanInputFormat, null, null, 0)
            start()
        }
    }

    /**
     * 步进式推进处理逻辑
     * @return 当前处理的 PTS (Us)，当音频处理彻底完成时返回 -1L
     */
    fun processFrame(
        extractor: MediaExtractor,
        trackIndex: Int,
        muxerWrapper: MuxerWrapper
    ): Long {
        if (isEncoderEOS || (isExtractorEOS && config.isPassthrough)) return -1L

        return if (config.isPassthrough) {
            processPassthroughFrame(extractor, trackIndex, muxerWrapper)
        } else {
            processReEncodeFrame(extractor, trackIndex, muxerWrapper)
        }
    }

    /**
     * 透传模式处理（直接写 Muxer，不经过 Codec 与 Filter）
     */
    private fun processPassthroughFrame(
        extractor: MediaExtractor,
        trackIndex: Int,
        muxerWrapper: MuxerWrapper
    ): Long {
        val buffer = passthroughBuffer ?: return -1L

        if (!isTrackAdded) {
            val inputFormat = extractor.getTrackFormat(trackIndex)
            muxerTrackIndex = muxerWrapper.addTrack(inputFormat)
            isTrackAdded = true
        }

        buffer.clear()
        val sampleSize = extractor.readSampleData(buffer, 0)

        if (sampleSize < 0) {
            isExtractorEOS = true
            Log.d(TAG, "Audio Passthrough reached EOS.")
            return -1L
        }

        val sampleTime = extractor.sampleTime
        val extractorFlags = extractor.sampleFlags

        var codecFlags = 0
        if ((extractorFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
            codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_KEY_FRAME
        }
        if ((extractorFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
            codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
        }

        bufferInfo.set(0, sampleSize, sampleTime, codecFlags)
        muxerWrapper.writeSampleData(muxerTrackIndex, buffer, bufferInfo)

        extractor.advance()
        return sampleTime
    }

    /**
     * 重编码模式处理（经典三段式 Pipe）
     */
    private fun processReEncodeFrame(
        extractor: MediaExtractor,
        trackIndex: Int,
        muxerWrapper: MuxerWrapper
    ): Long {
        var currentPresentationTimeUs = 0L

        // 步骤 1: Extractor -> Decoder Input
        pumpExtractorToDecoder(extractor)

        // 步骤 2: Decoder Output (PCM) -> Filter Chain -> PcmFrameAligner -> Encoder Input
        pumpDecoderToEncoder()

        // 步骤 3: Encoder Output -> Muxer
        val encodedPts = pumpEncoderToMuxer(muxerWrapper)
        if (encodedPts > 0) {
            currentPresentationTimeUs = encodedPts
        }

        return currentPresentationTimeUs
    }

    /**
     * 第一阶段：解封装数据送入解码器
     */
    private fun pumpExtractorToDecoder(extractor: MediaExtractor) {
        if (isExtractorEOS) return

        val inputIndex = decoder!!.dequeueInputBuffer(TIMEOUT_US)
        if (inputIndex >= 0) {
            val inputBuffer = decoder!!.getInputBuffer(inputIndex) ?: return
            val sampleSize = extractor.readSampleData(inputBuffer, 0)

            if (sampleSize < 0) {
                decoder!!.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                isExtractorEOS = true
                Log.d(TAG, "Audio Extractor reached EOS, queued EOS to Decoder.")
            } else {
                val sampleTime = extractor.sampleTime
                decoder!!.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, 0)
                extractor.advance()
            }
        }
    }

    /**
     * 第二阶段：解封装 PCM -> 经过责任链加工 -> 经过切片对齐器 -> 送入编码器
     */
    private fun pumpDecoderToEncoder() {
        if (isDecoderEOS) return

        val decoderStatus = decoder!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)

        if (decoderStatus >= 0) {
            val pcmBuffer = decoder!!.getOutputBuffer(decoderStatus)
            val isEosFrame = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0

            if (pcmBuffer != null && bufferInfo.size > 0) {
                // 1. 精确截取当前解码产生的有效 PCM 区域
                pcmBuffer.position(bufferInfo.offset)
                pcmBuffer.limit(bufferInfo.offset + bufferInfo.size)

                // 2. 将 Raw PCM 传给 FilterChain 责任链加工（例如重采样、变速、调节音量）
                val filteredBuffer = filterChain.process(pcmBuffer)

                // 3. 将加工后的数据投递给切片器，切成标准的 4096 字节大块并推算精确定时 PTS
                frameAligner?.pushAndAlign(
                    inputBuffer = filteredBuffer,
                    currentInputPtsUs = bufferInfo.presentationTimeUs
                ) { chunkBuffer, framePtsUs ->
                    // 回调送入硬件编码器
                    feedBufferToEncoder(chunkBuffer, framePtsUs, false)
                }
            }

            // 处理 EOS 边界
            if (isEosFrame) {
                flushFilterAndAlignerPipeline()
                isDecoderEOS = true
                Log.d(TAG, "Audio Decoder output reached EOS.")
            }

            decoder!!.releaseOutputBuffer(decoderStatus, false)

        } else if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
            if (isExtractorEOS) {
                audioRetryCount++
                if (audioRetryCount > MAX_RETRY_COUNT) {
                    isDecoderEOS = true
                    Log.w(TAG, "Audio Decoder output stuck, force flushing and signalling EOS.")
                    flushFilterAndAlignerPipeline()
                }
            }
        }
    }

    /**
     * 刷空 FilterChain 和 Aligner 中的尾部缓存数据并标记 EOS
     */
    private fun flushFilterAndAlignerPipeline() {
        // 1. 刷空 Filter 内部残留（如 FFmpeg swresample 尾部 sample）
        val flushedBuffer = filterChain.flush()
        if (flushedBuffer != null && flushedBuffer.hasRemaining()) {
            frameAligner?.pushAndAlign(
                inputBuffer = flushedBuffer,
                currentInputPtsUs = -1L
            ) { chunkBuffer, framePtsUs ->
                feedBufferToEncoder(chunkBuffer, framePtsUs, false)
            }
        }

        // 2. 刷空切片器尾部不足一帧的残余，补全静音包并触发最终 EOS 标记
        frameAligner?.flushRemaining { chunkBuffer, framePtsUs ->
            feedBufferToEncoder(chunkBuffer, framePtsUs, false)
        }

        // 3. 给硬件编码器发送最终的 EOS Flag
        sendEosToEncoder()
    }

    /**
     * 将 4096 字节的对齐 PCM 块可靠写入编码器 Input Buffer
     */
    private fun feedBufferToEncoder(buffer: ByteBuffer, ptsUs: Long, isEos: Boolean) {
        var queued = false
        var tryTimes = 0
        val sizeToQueue = buffer.remaining()

        while (!queued && tryTimes < 10) {
            val encoderInputIndex = encoder!!.dequeueInputBuffer(TIMEOUT_US)
            if (encoderInputIndex >= 0) {
                val encoderInputBuffer = encoder!!.getInputBuffer(encoderInputIndex)
                if (encoderInputBuffer != null) {
                    encoderInputBuffer.clear()
                    encoderInputBuffer.put(buffer)
                    encoder!!.queueInputBuffer(
                        encoderInputIndex,
                        0,
                        sizeToQueue,
                        ptsUs,
                        if (isEos) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                    )
                    queued = true
                }
            } else {
                tryTimes++
                Thread.yield() // 出让 CPU 资源，规避无谓的 CPU 占用
            }
        }
        if (!queued) {
            Log.e(TAG, "Failed to queue audio buffer to encoder after max retries! Buffer lost size=$sizeToQueue")
        }
    }

    /**
     * 安全发送 EOS 标记给编码器
     */
    private fun sendEosToEncoder() {
        var sent = false
        var retry = 0
        while (!sent && retry < 10) {
            val encoderInputIndex = encoder!!.dequeueInputBuffer(TIMEOUT_US)
            if (encoderInputIndex >= 0) {
                encoder!!.queueInputBuffer(
                    encoderInputIndex,
                    0,
                    0,
                    0L,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
                sent = true
            }
            retry++
            Thread.yield()
        }
    }

    /**
     * 第三阶段：读取 AAC 编码数据写入 Muxer
     */
    private fun pumpEncoderToMuxer(muxerWrapper: MuxerWrapper): Long {
        var encodedPts = 0L

        while (true) {
            val encoderStatus = encoder!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)

            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (isTrackAdded) {
                    throw IllegalStateException("Audio format changed multiple times")
                }
                val newFormat = encoder!!.outputFormat
                muxerTrackIndex = muxerWrapper.addTrack(newFormat)
                isTrackAdded = true
                Log.d(TAG, "Audio encoder output format changed. Track added.")
            } else if (encoderStatus >= 0) {
                audioRetryCount = 0

                val encodedData = encoder!!.getOutputBuffer(encoderStatus)
                    ?: throw RuntimeException("Audio Encoder outputBuffer $encoderStatus was null")

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0
                }

                if (bufferInfo.size != 0 && isTrackAdded) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxerWrapper.writeSampleData(muxerTrackIndex, encodedData, bufferInfo)
                    encodedPts = bufferInfo.presentationTimeUs
                }

                encoder!!.releaseOutputBuffer(encoderStatus, false)

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "Audio Encoder output reached EOS naturally.")
                    isEncoderEOS = true
                    break
                }
            }
        }

        return encodedPts
    }

    fun isCompleted(): Boolean {
        return if (config.isPassthrough) {
            isExtractorEOS
        } else {
            isEncoderEOS
        }
    }

    fun release() {
        runCatching { filterChain.release() }
        runCatching { frameAligner?.reset() }
        runCatching { decoder?.stop(); decoder?.release() }
        runCatching { encoder?.stop(); encoder?.release() }
        passthroughBuffer?.clear()
        passthroughBuffer = null
        decoder = null
        encoder = null
        frameAligner = null
    }

    companion object {
        private const val TAG = "AudioTrackProcessor"
        private const val TIMEOUT_US = 2500L
        private const val MAX_RETRY_COUNT = 60
    }
}