package com.example.audioandvideoeditor.model

import android.media.AudioFormat

data class AudioFormatParams(
    val sampleRate: Int,
    val channelCount: Int,
    val pcmEncoding: Int = AudioFormat.ENCODING_PCM_16BIT
) {
    /** 每帧 Sample 的单通道字节数 */
    val bytesPerSample: Int = when (pcmEncoding) {
        AudioFormat.ENCODING_PCM_16BIT -> 2
        AudioFormat.ENCODING_PCM_FLOAT -> 4
        AudioFormat.ENCODING_PCM_8BIT  -> 1
        else -> 2 // 默认按 16bit 计算
    }

    /** 单个 Sample 所有通道的总字节大小 (例如双声道 16bit = 4 bytes) */
    val frameSize: Int = channelCount * bytesPerSample

    /** 转换为 FFmpeg AVSampleFormat 枚举整数 */
    val ffmpegSampleFmt: Int = when (pcmEncoding) {
        AudioFormat.ENCODING_PCM_16BIT -> 1 // AV_SAMPLE_FMT_S16
        AudioFormat.ENCODING_PCM_FLOAT -> 3 // AV_SAMPLE_FMT_FLT
        AudioFormat.ENCODING_PCM_8BIT  -> 0 // AV_SAMPLE_FMT_U8
        else -> 1
    }
}