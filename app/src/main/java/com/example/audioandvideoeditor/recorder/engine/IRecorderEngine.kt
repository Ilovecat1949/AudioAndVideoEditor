package com.example.audioandvideoeditor.recorder.engine

import android.media.projection.MediaProjection
import android.os.ParcelFileDescriptor
import com.example.audioandvideoeditor.model.RecordingConfig

interface IRecorderEngine {
    fun start(config: RecordingConfig, projection: MediaProjection, pfd: ParcelFileDescriptor)
    fun pause()
    fun resume()
    fun stop()
    fun release()
}