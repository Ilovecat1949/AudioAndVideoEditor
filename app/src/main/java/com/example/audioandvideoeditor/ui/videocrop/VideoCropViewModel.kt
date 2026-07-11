package com.example.audioandvideoeditor.ui.videocrop

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.audioandvideoeditor.application.AppApplication
import com.example.audioandvideoeditor.entity.MediaInfo

class VideoCropViewModel : ViewModel() {
    data class Rect(var start: Float, var top: Float, var width: Float, var height: Float)
    var video_rect by mutableStateOf(Rect(0f, 0f, 0f, 0f,))
    var crop_rect by mutableStateOf(Rect(0f, 0f, 0f, 0f))
    var crop_start by mutableStateOf(
        0f
    )
    var crop_top by mutableStateOf(0f)
    var crop_width by mutableStateOf(1f)
    var crop_height by mutableStateOf(1f)
    val info= MediaInfo()
    var currentVideoUri by mutableStateOf<Uri?>(null)
        private set

    private var exoPlayer: ExoPlayer? = null

    fun setVideoUri(uri: Uri?) {
        currentVideoUri = uri
    }
    var initialize_source_flag by mutableStateOf(false)
    fun initializeSource(context: Context) {
        if(currentVideoUri != null) {
            val text = AppApplication.INSTANCE.taskRepository.getAVInfo(currentVideoUri!!.path!!)
            info.initInfo(text)
            videoDimensions=getNewVideoDimensions(screenWidth,screenHeight,info.width,info.height)
            video_rect=Rect((screenWidth-videoDimensions.first)/2f, (screenHeight-videoDimensions.second)/2f, videoDimensions.first*1f, videoDimensions.second*1f)
            crop_rect =Rect(video_rect.start+video_rect.width*1/3, video_rect.top+video_rect.height*1/3, video_rect.width*1/3, video_rect.height*1/3)
            crop_start=1f/3
            crop_top=1f/3
            crop_width=1f/3
            crop_height=1f/3
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

    fun getDuration(): Long{
        return exoPlayer?.duration?: 0L
    }
    var screenWidth: Int=0
    var screenHeight: Int=0
    var videoDimensions by mutableStateOf(Pair(0, 0))
    fun getNewVideoDimensions(
        width:Int,
        heigth:Int,
        a:Int,
        b:Int
    ): Pair<Int, Int> {
        val ratio=a*1f/b
        val h_max = Math.min(heigth * 1f, width / ratio)
        val new_w = ratio * h_max
        return Pair(new_w.toInt(), h_max.toInt())
    }

    var editFileNameFlag = mutableStateOf(false)
    var target_name=""
}