package com.example.audioandvideoeditor.model

/**
 * 滤镜类型统一契约接口
 */
interface IFilterType {
    val nativeId: Int // 映射到 C++ Native 层的整型 ID
}

/**
 * 音频滤镜类型枚举
 */
enum class AudioFilterType(override val nativeId: Int) : IFilterType {
    PASSTHROUGH(0), // 透传/无操作
    RESAMPLE(1),    // 重采样 & 声道转换 (FFmpeg Swr)
    ATEMPO(2),      // 音频变速 (FFmpeg libavfilter atempo)
    VOLUME(3);      // 音量调节

    companion object {
        fun fromNativeId(id: Int): AudioFilterType =
            entries.firstOrNull { it.nativeId == id } ?: PASSTHROUGH
    }
}

/**
 * 视频滤镜类型枚举
 */
enum class VideoFilterType(override val nativeId: Int) : IFilterType {
    PASSTHROUGH(0),  // 默认纹理绘制
    CROP(101),       // 画面裁剪
    WATERMARK(102),  // 水印/贴纸 overlay
    LUT_COLOR(103);  // LUT 色彩滤镜

    companion object {
        fun fromNativeId(id: Int): VideoFilterType =
            entries.firstOrNull { it.nativeId == id } ?: PASSTHROUGH
    }
}