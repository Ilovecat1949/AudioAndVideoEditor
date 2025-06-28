package com.example.audioandvideoeditor.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class VideoSegmenterViewModel : ViewModel() {
    var currentVideoUri by mutableStateOf<Uri?>(null)
        private set

    private var exoPlayer: ExoPlayer? = null

    fun setVideoUri(uri: Uri?) {
        currentVideoUri = uri
    }
    var initialize_source_flag by mutableStateOf(false)
    fun initializeSource(context: android.content.Context) {
        if (exoPlayer == null && currentVideoUri != null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(currentVideoUri!!)
                setMediaItem(mediaItem)
                prepare()
            }
        }
        initialize_source_flag=true
    }

    fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    fun getExoPlayer(): ExoPlayer? {
        return exoPlayer
    }

    fun getDuration(): Long{
        return exoPlayer?.duration?: 0L
    }

    fun getCurrentPosition(): Float {
        return exoPlayer?.currentPosition?.toFloat() ?: 0f
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying ?: false
    }

    fun seekTo(position: Float) {
        exoPlayer?.seekTo(position.toLong())
    }

    var startTime by mutableStateOf(0f)
    var endTime by mutableStateOf(1f)

    fun seekToFraction(fraction: Float) {
        val duration = exoPlayer?.duration?.toFloat() ?: 0f
        val position = duration * fraction
        exoPlayer?.seekTo(position.toLong())
    }

    fun updateEndTimeAndSeek(newEndTime: Float) {
        endTime = if (newEndTime <= startTime) {
            startTime
        } else {
            newEndTime
        }
        seekToFraction(endTime) // Directly seek after updating
    }

    fun updateStartTimeAndSeek(newStartTime: Float) {
        startTime = if (newStartTime >= endTime) {
            endTime
        } else {
            newStartTime
        }
        seekToFraction(startTime) // Directly seek after updating
    }
    var showTimeErrorDialog by mutableStateOf(false) // In the ViewModel
    var croppingMode by mutableStateOf<CroppingMode>(CroppingMode.Quick)
    enum class CroppingMode {
        Quick,
        Precise
    }
    var outputFileName by mutableStateOf("")
    var showFileNameDialog by mutableStateOf(false)
}

