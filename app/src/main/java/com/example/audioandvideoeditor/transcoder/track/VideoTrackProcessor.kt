package com.example.audioandvideoeditor.transcoder.track

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.opengl.EGLSurface
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import com.example.audioandvideoeditor.model.VideoEncodeConfig
import com.example.audioandvideoeditor.transcoder.video.gl.EglCore
import com.example.audioandvideoeditor.transcoder.infrastructure.MuxerWrapper
import com.example.audioandvideoeditor.transcoder.video.gl.TextureRender

/**
 * 优化后的视频轨硬件转码处理器（Clean Format + HandlerThread + EGL 1.4）
 * 采用无锁 Pipeline 架构：Extractor -> Decoder -> Surface/OpenGL -> Encoder -> Muxer
 */
class VideoTrackProcessor(
    private val config: VideoEncodeConfig
) {

    private var decoder: MediaCodec? = null
    private var encoder: MediaCodec? = null
    private var eglCore: EglCore? = null
    private var textureRender: TextureRender? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var encoderInputSurface: EGLSurface? = null
    private var glHandlerThread: HandlerThread? = null

    private var textureId = -1
    private var muxerTrackIndex = -1
    private var isTrackAdded = false

    private val transformMatrix = FloatArray(16)
    private val bufferInfo = MediaCodec.BufferInfo()

    // 状态控制标志位
    private var isDecoderInputEOS = false
    private var isDecoderOutputEOS = false
    private var isEncoderEOS = false

    private var encoderRetryCount = 0

    fun prepare(
        extractor: MediaExtractor,
        trackIndex: Int,
        inputFormat: MediaFormat
    ) {
        // 1. 配置编码器
        val encodeFormat = MediaFormat.createVideoFormat(
            config.mimeType,
            config.targetWidth,
            config.targetHeight
        ).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, config.bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, config.frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, config.iFrameInterval)
            setInteger(MediaFormat.KEY_BITRATE_MODE, config.bitrateMode)

            // 3. 高级压缩工具链 (提升压缩率，同等码率画质更好/体积更小)
            setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
            // 4. B 帧优化 (Android 7.0+ 提效)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setInteger(MediaFormat.KEY_MAX_B_FRAMES, 1)
            }
        }

        encoder = MediaCodec.createEncoderByType(config.mimeType).apply {
            configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }

        // 2. 初始化 EGL 环境与 Input Surface
        val rawInputSurface: Surface = encoder!!.createInputSurface()
        eglCore = EglCore()
        encoderInputSurface = eglCore!!.createWindowSurface(rawInputSurface)
        eglCore!!.makeCurrent(encoderInputSurface!!)

        // 3. 创建 OpenGL 渲染器与 SurfaceTexture
        textureRender = TextureRender().apply {
            surfaceCreated()
            textureId = createTextureObject()
        }

        // 绑定稳定的 HandlerThread，确保底层 BufferQueue Sync Fence 正常信号化
        glHandlerThread = HandlerThread("VideoGLThread").apply { start() }
        val glHandler = Handler(glHandlerThread!!.looper)

        surfaceTexture = SurfaceTexture(textureId).apply {
            setOnFrameAvailableListener({
                // 仅作为 BufferQueue 内部信号触达的 Looper 宿主
            }, glHandler)
        }
        val decoderSurface = Surface(surfaceTexture)

        // 4. 构建干净的 Clean Format，排除扩展色彩元数据（根治 ranchu 驱动报错）
        val mime = inputFormat.getString(MediaFormat.KEY_MIME)
            ?: throw IllegalArgumentException("Invalid MIME type")

        val cleanInputFormat = MediaFormat.createVideoFormat(
            mime,
            inputFormat.getInteger(MediaFormat.KEY_WIDTH),
            inputFormat.getInteger(MediaFormat.KEY_HEIGHT)
        ).apply {
            // 保留 CSD 参数（SPS/PPS 头）
            if (inputFormat.containsKey("csd-0")) {
                setByteBuffer("csd-0", inputFormat.getByteBuffer("csd-0"))
            }
            if (inputFormat.containsKey("csd-1")) {
                setByteBuffer("csd-1", inputFormat.getByteBuffer("csd-1"))
            }
        }

        decoder = MediaCodec.createDecoderByType(mime).apply {
            configure(cleanInputFormat, decoderSurface, null, 0)
            start()
        }
        encoder!!.start()
    }

    /**
     * 步进式推进处理逻辑
     * @return 当前处理的 PTS (Us)，当视频处理彻底完成时返回 -1L
     */
    fun processFrame(
        extractor: MediaExtractor,
        trackIndex: Int,
        muxerWrapper: MuxerWrapper
    ): Long {
        if (isEncoderEOS) return -1L

        var currentPresentationTimeUs = 0L

        // 步骤 1: Extractor -> Decoder Input
        pumpExtractorToDecoder(extractor)

        // 步骤 2: Decoder Output -> OpenGL -> Encoder Input
        val renderedPts = pumpDecoderToEncoder()
        if (renderedPts > 0) {
            currentPresentationTimeUs = renderedPts
        }

        // 步骤 3: Encoder Output -> Muxer
        pumpEncoderToMuxer(muxerWrapper)

        return currentPresentationTimeUs
    }

    private fun pumpExtractorToDecoder(extractor: MediaExtractor) {
        if (isDecoderInputEOS) return

        val inputIndex = decoder!!.dequeueInputBuffer(TIMEOUT_US)
        if (inputIndex >= 0) {
            val inputBuffer = decoder!!.getInputBuffer(inputIndex) ?: return
            val sampleSize = extractor.readSampleData(inputBuffer, 0)

            if (sampleSize < 0) {
                decoder!!.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                isDecoderInputEOS = true
                Log.d(TAG, "Extractor reached EOS, queued EOS to Decoder.")
            } else {
                val sampleTime = extractor.sampleTime
                decoder!!.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, 0)
                extractor.advance()
            }
        }
    }

    private fun pumpDecoderToEncoder(): Long {
        if (isDecoderOutputEOS) return 0L

        var renderedPts = 0L
        val decoderStatus = decoder!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)

        if (decoderStatus >= 0) {
            val doRender = bufferInfo.size != 0

            if (doRender) {
                // 1. 释放 Buffer 给 Surface
                decoder!!.releaseOutputBuffer(decoderStatus, true)

                // 2. 更新并绘制纹理
                surfaceTexture!!.updateTexImage()

                eglCore!!.makeCurrent(encoderInputSurface!!)
                textureRender!!.drawFrame(
                    surfaceTexture!!,
                    textureId,
                    transformMatrix,
                    config.targetWidth,
                    config.targetHeight
                )

                // 3. 附带精确的 PTS 并触发 SwapBuffer 交给编码器
                eglCore!!.setPresentationTime(encoderInputSurface!!, bufferInfo.presentationTimeUs * 1000)
                eglCore!!.swapBuffers(encoderInputSurface!!)

                renderedPts = bufferInfo.presentationTimeUs
            } else {
                decoder!!.releaseOutputBuffer(decoderStatus, false)
            }

            // 判断解码器是否到达 EOS
            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                isDecoderOutputEOS = true
                Log.d(TAG, "Decoder output reached EOS, signalling EOS to encoder.")
                runCatching { encoder!!.signalEndOfInputStream() }
            }
        } else if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
            if (isDecoderInputEOS) {
                encoderRetryCount++
                if (encoderRetryCount > MAX_RETRY_COUNT) {
                    isDecoderOutputEOS = true
                    Log.w(TAG, "Decoder output stuck after input EOS, force signalling EOS to encoder.")
                    runCatching { encoder!!.signalEndOfInputStream() }
                }
            }
        }

        return renderedPts
    }

    private fun pumpEncoderToMuxer(muxerWrapper: MuxerWrapper) {
        while (true) {
            val encoderStatus = encoder!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)

            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (isTrackAdded) {
                    throw IllegalStateException("Video format changed multiple times")
                }
                val newFormat = encoder!!.outputFormat
                muxerTrackIndex = muxerWrapper.addTrack(newFormat)
                isTrackAdded = true
                Log.d(TAG, "Video encoder output format changed. Track added.")
            } else if (encoderStatus >= 0) {
                encoderRetryCount = 0

                val encodedData = encoder!!.getOutputBuffer(encoderStatus)
                    ?: throw RuntimeException("Encoder outputBuffer $encoderStatus was null")

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0
                }

                if (bufferInfo.size != 0 && isTrackAdded) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxerWrapper.writeSampleData(muxerTrackIndex, encodedData, bufferInfo)
                }

                encoder!!.releaseOutputBuffer(encoderStatus, false)

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "Encoder output reached EOS naturally.")
                    isEncoderEOS = true
                    break
                }
            }
        }
    }

    fun isCompleted(): Boolean = isEncoderEOS

    /**
     * 严格按逆序销毁资源，防止挂死或逻辑泄漏
     */
    fun release() {
        runCatching { decoder?.stop(); decoder?.release() }
        runCatching { encoder?.stop(); encoder?.release() }

        runCatching {
            eglCore?.makeNothingCurrent()
            if (encoderInputSurface != null) {
                eglCore?.releaseSurface(encoderInputSurface!!)
            }
            surfaceTexture?.release()
        }

        runCatching { textureRender?.release() }
        runCatching { eglCore?.release() }
        runCatching { glHandlerThread?.quitSafely() }

        decoder = null
        encoder = null
        eglCore = null
        textureRender = null
        surfaceTexture = null
        encoderInputSurface = null
        glHandlerThread = null
    }

    companion object {
        private const val TAG = "VideoTrackProcessor"
        private const val TIMEOUT_US = 2500L
        private const val MAX_RETRY_COUNT = 60
    }
}