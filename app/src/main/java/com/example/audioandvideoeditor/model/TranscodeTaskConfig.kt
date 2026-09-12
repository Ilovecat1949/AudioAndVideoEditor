package com.example.audioandvideoeditor.model

/**
 * 转码任务总配置（组合模式）
 */
data class TranscodeTaskConfig(
    val videoConfig: VideoEncodeConfig?,
    val audioConfig: AudioEncodeConfig?
)