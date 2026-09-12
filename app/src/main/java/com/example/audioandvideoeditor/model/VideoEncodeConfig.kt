package com.example.audioandvideoeditor.model

import android.media.MediaCodecInfo

/**
 * 视频硬件编码/转码配置模型
 */
data class VideoEncodeConfig(
    val targetWidth: Int,
    val targetHeight: Int,
    val bitrate: Int,
    val frameRate: Int = 30,
    val iFrameInterval: Int = 1,
    val bitrateMode: Int = MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR,
    val mimeType: String = "video/avc"
) {
    init {
        require(targetWidth % 2 == 0 && targetHeight % 2 == 0) {
            "Video encoding requires even dimensions: ${targetWidth}x${targetHeight}"
        }
        require(bitrate > 0) { "Bitrate must be greater than 0" }
        require(frameRate > 0) { "Frame rate must be greater than 0" }
    }
}