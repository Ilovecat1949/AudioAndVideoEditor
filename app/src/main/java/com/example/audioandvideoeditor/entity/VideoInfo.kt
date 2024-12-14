package com.example.audioandvideoeditor.entity

import android.net.Uri

data class VideoInfo(
    val id:Long,
    val uri: Uri,
    val path:String,
    val name: String,
    val duration: Int,
    val size: Int
)
