package com.example.audioandvideoeditor.model

import android.graphics.RectF
import android.media.MediaCodecInfo


/**
 * 填充与缩放模式
 */
enum class ScaleMode {
    FIT_CENTER,
    CENTER_CROP,
    FILL
}
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
    val mimeType: String = "video/avc",

// 扩展视频画面/特效参数
    val scaleMode: ScaleMode = ScaleMode.FIT_CENTER,
    val cropRect: RectF? = null,
    val rotation: Int = 0,
    val speed: Float = 1.0f
) {
    init {
        require(targetWidth % 2 == 0 && targetHeight % 2 == 0) {
            "Video encoding requires even dimensions: ${targetWidth}x${targetHeight}"
        }
        require(bitrate > 0) { "Bitrate must be greater than 0" }
        require(frameRate > 0) { "Frame rate must be greater than 0" }

        require(rotation in setOf(0, 90, 180, 270)) { "Rotation must be 0, 90, 180, or 270 degrees" }
        require(speed in 0.25f..4.0f) { "Video speed must be between 0.25 and 4.0" }
        if (cropRect != null) {
            require(
                cropRect.left in 0.0f..1.0f && cropRect.top in 0.0f..1.0f &&
                        cropRect.right in 0.0f..1.0f && cropRect.bottom in 0.0f..1.0f
            ) { "Crop rect coordinates must be normalized within [0.0, 1.0]" }
        }
    }
}