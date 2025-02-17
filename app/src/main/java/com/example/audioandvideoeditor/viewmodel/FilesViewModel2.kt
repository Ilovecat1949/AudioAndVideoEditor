package com.example.audioandvideoeditor.viewmodel

import android.content.ContentResolver
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.example.audioandvideoeditor.dao.AudiosPagingSource
import com.example.audioandvideoeditor.dao.VideosPagingSource
import com.example.audioandvideoeditor.entity.AudioInfo
import com.example.audioandvideoeditor.entity.VideoInfo
import java.io.File

class FilesViewModel2 : ViewModel()  {
    private val pagerSize=5000//100
    private var videosSource: VideosPagingSource = VideosPagingSource()
    private var audiosSource: AudiosPagingSource = AudiosPagingSource()
    fun setContentResolver(contentResolver: ContentResolver){
        videosSource.setContentResolver(contentResolver)
        audiosSource.setContentResolver(contentResolver)
    }
    val show_flag= mutableStateOf(0)
    val show_details_flag= mutableStateOf(0)
    val  filesList= mutableStateListOf<File>()
    var file:File?=null
    var parent: File?=null
    var audioInfo: AudioInfo?=null
    var videoInfo: VideoInfo?=null
    val videosPager= Pager(
        // Configure how data is loaded by passing additional properties to
        // PagingConfig, such as prefetchDistance.
        PagingConfig(pageSize = pagerSize, enablePlaceholders = true)
    ) {
        videosSource
    }
    val audiosPager= Pager(
        // Configure how data is loaded by passing additional properties to
        // PagingConfig, such as prefetchDistance.
        PagingConfig(pageSize = pagerSize, enablePlaceholders = true)
    ) {
        audiosSource
    }
    lateinit var videoPlay:(file: File, route:String)->Unit
}