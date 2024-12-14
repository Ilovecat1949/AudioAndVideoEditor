package com.example.audioandvideoeditor.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.media3.exoplayer.ExoPlayer

class VideosPlayViewModel: ViewModel()  {
    lateinit var exoplayer: ExoPlayer
    private var init_exoplayer_flag=true
    var path=""
    fun initExoPlayer(ctx:Context){
//        if(init_exoplayer_flag){
            exoplayer= ExoPlayer.Builder(ctx)
                .build()
                .apply {
                    playWhenReady = false
                }
//            init_exoplayer_flag=false
//        }
    }

}