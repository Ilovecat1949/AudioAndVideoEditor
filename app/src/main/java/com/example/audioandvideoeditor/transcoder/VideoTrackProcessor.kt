package com.example.audioandvideoeditor.transcoder

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.EGLSurface
import android.os.Build
import android.util.Log
import android.view.Surface
import com.example.audioandvideoeditor.model.VideoEncodeConfig

/**
 * 视频轨硬件转码处理器
 * 基于 Drain Loop (同步阻塞循环) 模式实现：Extractor -> Decoder -> Surface/OpenGL -> Encoder -> Muxer
 */
class VideoTrackProcessor(
    private val config: VideoEncodeConfig
) : SurfaceTexture.OnFrameAvailableListener {

    private var decoder: MediaCodec? = null
    private var encoder: MediaCodec? = null
    private var eglCore: EglCore? = null
    private var textureRender: TextureRender? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var encoderInputSurface: EGLSurface? = null

    private var textureId = -1
    private var muxerTrackIndex = -1
    private var isMuxerStarted = false

    @Volatile
    private var frameAvailable = false
    private val frameSyncObject = Object()

    private val transformMatrix = FloatArray(16)
    private val bufferInfo = MediaCodec.BufferInfo()

    private var isDecoderEOS = false
    private var isEncoderEOS = false

    /**
     * 初始化编解码器与 OpenGL EGL 渲染管道
     */
    fun prepare(
        extractor: MediaExtractor,
        trackIndex: Int,
        inputFormat: MediaFormat
    ) {
        // 1. 创建硬件编码器及 Input Surface
        val encodeFormat = MediaFormat.createVideoFormat(
            config.mimeType,
            config.targetWidth,
            config.targetHeight
        ).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, config.bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, config.frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, config.iFrameInterval)
            setInteger(MediaFormat.KEY_BITRATE_MODE, config.bitrateMode)
        }

        encoder = MediaCodec.createEncoderByType(config.mimeType).apply {
            configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }

        val rawInputSurface: Surface = encoder!!.createInputSurface()

        // 2. 初始化 EGL 与 离屏纹理渲染器
        eglCore = EglCore()
        encoderInputSurface = eglCore!!.createWindowSurface(rawInputSurface)
        eglCore!!.makeCurrent(encoderInputSurface!!)

        textureRender = TextureRender().apply {
            surfaceCreated()
            textureId = createTextureObject()
        }

        // 3. 创建 SurfaceTexture 绑定解码器输出
        surfaceTexture = SurfaceTexture(textureId).apply {
            setOnFrameAvailableListener(this@VideoTrackProcessor)
        }

        val decoderSurface = Surface(surfaceTexture)

        // 4. 创建解码器并关联 Surface
        val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: throw IllegalArgumentException("Invalid MIME type")
        decoder = MediaCodec.createDecoderByType(mime).apply {
            configure(inputFormat, decoderSurface, null, 0)
            start()
        }
        encoder!!.start()
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        synchronized(frameSyncObject) {
            if (frameAvailable) {
                Log.w(TAG, "frameAvailable already set, frame may be dropped")
            }
            frameAvailable = true
            frameSyncObject.notifyAll()
        }
    }

    /**
     * 单步推流处理（在后台线程中被循环调用）
     * @return 当前已处理视频帧的 PTS (微秒)，用于计算进度；若转码结束返回 -1L
     */
    fun processFrame(
        extractor: MediaExtractor,
        trackIndex: Int,
        muxer: MediaMuxer,
        onMuxerStartCheck: (MediaFormat) -> Int
    ): Long {
        if (isEncoderEOS) return -1L

        var currentPresentationTimeUs = 0L

        // Step 1: 从 Extractor 读取数据喂给 Decoder
        if (!isDecoderEOS) {
            val inputIndex = decoder!!.dequeueInputBuffer(TIMEOUT_US)
            if (inputIndex >= 0) {
                val inputBuffer = decoder!!.getInputBuffer(inputIndex)
                if (inputBuffer != null) {
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        decoder!!.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isDecoderEOS = true
                    } else {
                        val sampleTime = extractor.sampleTime
                        decoder!!.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
        }

        // Step 2: 从 Decoder 拉取解码帧 -> Surface
        val decoderStatus = decoder!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
        if (decoderStatus >= 0) {
            val doRender = bufferInfo.size != 0
            decoder!!.releaseOutputBuffer(decoderStatus, doRender)

            if (doRender) {
                awaitNewImage()
                surfaceTexture!!.updateTexImage()

                // OpenGL 绘制与图像拉伸
                eglCore!!.makeCurrent(encoderInputSurface!!)
                textureRender!!.drawFrame(surfaceTexture!!, textureId, transformMatrix)

                // 注入原始 PTS 时间戳并提交编码器 (注意：eglPresentationTimeANDROID 单位是纳秒)
                eglCore!!.setPresentationTime(encoderInputSurface!!, bufferInfo.presentationTimeUs * 1000)
                eglCore!!.swapBuffers(encoderInputSurface!!)
                currentPresentationTimeUs = bufferInfo.presentationTimeUs
            }

            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                encoder!!.signalEndOfInputStream()
            }
        }

        // Step 3: 从 Encoder 拉取编码完成的数据 -> Muxer
        drainEncoder(muxer, onMuxerStartCheck)

        return currentPresentationTimeUs
    }

    private fun awaitNewImage() {
        synchronized(frameSyncObject) {
            while (!frameAvailable) {
                try {
                    frameSyncObject.wait(500)
                    if (!frameAvailable) {
                        throw RuntimeException("Surface frame wait timed out")
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw RuntimeException(e)
                }
            }
            frameAvailable = false
        }
    }

    private fun drainEncoder(muxer: MediaMuxer, onMuxerStartCheck: (MediaFormat) -> Int) {
        while (true) {
            val encoderStatus = encoder!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (isMuxerStarted) {
                    throw RuntimeException("format changed twice")
                }
                val newFormat = encoder!!.outputFormat
                muxerTrackIndex = onMuxerStartCheck(newFormat)
                isMuxerStarted = true
            } else if (encoderStatus >= 0) {
                val encodedData = encoder!!.getOutputBuffer(encoderStatus)
                    ?: throw RuntimeException("encoderOutputBuffer $encoderStatus was null")

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0
                }

                if (bufferInfo.size != 0 && isMuxerStarted) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(muxerTrackIndex, encodedData, bufferInfo)
                }

                encoder!!.releaseOutputBuffer(encoderStatus, false)

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    isEncoderEOS = true
                    break
                }
            }
        }
    }

    fun release() {
        runCatching { decoder?.stop(); decoder?.release() }
        runCatching { encoder?.stop(); encoder?.release() }
        runCatching {
            if (encoderInputSurface != null) eglCore?.releaseSurface(encoderInputSurface!!)
            eglCore?.release()
        }
        surfaceTexture?.release()
        decoder = null
        encoder = null
        eglCore = null
        textureRender = null
        surfaceTexture = null
    }

    companion object {
        private const val TAG = "VideoTrackProcessor"
        private const val TIMEOUT_US = 2500L
    }
}