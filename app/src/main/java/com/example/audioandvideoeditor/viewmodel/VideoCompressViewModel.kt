package com.example.audioandvideoeditor.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.audioandvideoeditor.entity.MediaInfo
import com.example.audioandvideoeditor.services.TasksBinder

class VideoCompressViewModel: ViewModel() {
    val info= MediaInfo()
    lateinit var tasksBinder: TasksBinder
    var currentVideoUri by mutableStateOf<Uri?>(null)
        private set
    private var exoPlayer: ExoPlayer? = null
    fun setVideoUri(uri: Uri?) {
        currentVideoUri = uri
    }
    var initialize_source_flag by mutableStateOf(false)
    fun initializeSource(context:Context) {
        if(currentVideoUri != null) {
            val text = tasksBinder.getAVInfo(currentVideoUri!!.path!!)
            info.initInfo(text)

        }
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
}