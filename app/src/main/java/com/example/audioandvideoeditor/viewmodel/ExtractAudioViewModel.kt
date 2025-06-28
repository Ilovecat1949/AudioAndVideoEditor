package com.example.audioandvideoeditor.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class ExtractAudioViewModel : ViewModel()  {
    var currentVideoUri by mutableStateOf<Uri?>(null)
        private set

    private var exoPlayer: ExoPlayer? = null
    var initialize_source_flag by mutableStateOf(false)
    fun initializeSource(context: android.content.Context){
        if (exoPlayer == null && currentVideoUri != null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(currentVideoUri!!)
                setMediaItem(mediaItem)
                prepare()
            }
        }
        initialize_source_flag=true
    }
    fun setVideoUri(uri: Uri?) {
        currentVideoUri = uri
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
    var editFileNameFlag = mutableStateOf(false)
    var target_name=""

    val targetFormatOptions = listOf("AAC","FLAC","WAV","OGG","M4A","AC3","EAC3",)//"AVI"   ,"MPEG" "MP2" ,"AIFF" ,"OPUS" ,"WMA"
    val targetFormatText= mutableStateOf("AAC")
    val changeTargetFormatFlag=mutableStateOf(false)
    val checkTargetFormatFlag=mutableStateOf(0)
}