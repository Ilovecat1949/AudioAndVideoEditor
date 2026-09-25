package com.example.audioandvideoeditor.model

import android.media.AudioFormat

/**
 * 音频硬件编码/重编码配置模型
 */
data class AudioEncodeConfig(
    val bitrate: Int = 128_000,
    val sampleRate: Int = 44100,
    val channelCount: Int = 2,
    val pcmEncoding: Int = AudioFormat.ENCODING_PCM_16BIT,
    val isPassthrough: Boolean = false,
    val mimeType: String = "audio/mp4a-latm",

    // 扩展音频特效/ DSP 参数
    val volume: Float = 1.0f,
    val speed: Float = 1.0f,
    val isMuted: Boolean = false
)