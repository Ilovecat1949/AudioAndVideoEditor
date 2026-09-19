package com.example.audioandvideoeditor.model

import android.media.AudioFormat

data class AudioFormatParams(
    val sampleRate: Int,
    val channelCount: Int,
    val pcmEncoding: Int = AudioFormat.ENCODING_PCM_16BIT
) {
    /** 每帧 Sample 的单通道字节数 (16bit = 2 bytes) */
    val bytesPerSample: Int = if (pcmEncoding == AudioFormat.ENCODING_PCM_16BIT) 2 else 1

    /** 单个 Sample 所有通道的总字节大小 (例如双声道 16bit = 4 bytes) */
    val frameSize: Int = channelCount * bytesPerSample
}