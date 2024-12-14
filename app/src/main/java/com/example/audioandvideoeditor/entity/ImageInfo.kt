package com.example.audioandvideoeditor.entity

import android.net.Uri

data class ImageInfo(
    val id:Long,
    val uri: Uri,
    val path:String,
    val name: String,
    val height: Int,
    val width:Int,
    val size: Int
)
