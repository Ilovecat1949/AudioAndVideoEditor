package com.example.audioandvideoeditor.model

import android.os.Build

enum class AudioSourceOption {
    NONE,       // 静音
    MIC,        // 仅麦克风
    INTERNAL,   // 仅系统内录 (需 API 29+)
    MIXED;      // 麦克风 + 内录混合 (需 API 29+)

    companion object {
        fun isSupported(option: AudioSourceOption): Boolean {
            return when (option) {
                INTERNAL, MIXED -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                else -> true
            }
        }
    }
}