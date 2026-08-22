package com.example.audioandvideoeditor.recorder.engine

import android.content.Context
import com.example.audioandvideoeditor.model.AudioSourceOption
import com.example.audioandvideoeditor.model.RecordingConfig

object RecorderEngineFactory {
    fun createEngine(context: Context, config: RecordingConfig): IRecorderEngine {
        return when (config.audioOption) {
            // 需要采集系统内录或双路混音时，切换至 MediaCodec + MediaMuxer 引擎
            AudioSourceOption.INTERNAL,
            AudioSourceOption.MIXED -> MediaCodecEngine(context)

            // 无声或单麦克风场景，走低功耗系统的 MediaRecorder 引擎
            AudioSourceOption.NONE,
            AudioSourceOption.MIC -> MediaRecorderEngine(context)
        }
    }
}