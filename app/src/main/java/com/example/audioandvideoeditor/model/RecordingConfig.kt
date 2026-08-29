package com.example.audioandvideoeditor.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecordingConfig(
    val videoWidth: Int = 0,               // 0 代表跟随屏幕物理分辨率
    val videoHeight: Int = 0,              // 0 代表跟随屏幕物理分辨率
    val videoBitrate: Int = 5 * 1024 * 1024, // 默认 5 Mbps
    val frameRate: Int = 30,               // 默认 30 fps
    val dpi: Int = 320,                    // 屏幕 DPI 密度
    val audioOption: AudioSourceOption = AudioSourceOption.INTERNAL, // 音源选项
    val sampleRate: Int = 44100,           // 默认 44.1 kHz
    val audioBitrate: Int = 128 * 1024     // 默认 128 kbps
) : Parcelable
