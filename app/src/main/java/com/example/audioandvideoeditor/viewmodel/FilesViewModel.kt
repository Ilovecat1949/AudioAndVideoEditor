package com.example.audioandvideoeditor.viewmodel

import android.content.ContentResolver
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.example.audioandvideoeditor.dao.AudiosPagingSource
import com.example.audioandvideoeditor.dao.VideosPagingSource
import java.io.File

class FilesViewModel : ViewModel() {
val  filesList= mutableStateListOf<File>()
val  filesState=HashMap<String,MutableState<Boolean>>()
var file:File?=null
var parent: File?=null
fun initFilesState(){
//    filesState.clear()
    filesList.forEach {
        if(!filesState.containsKey(it.path)) {
            filesState[it.path] = mutableStateOf(false)
        }
    }
}
fun updateFilesState(path:String, is_checked:Boolean) {
    filesState.forEach {
        it.value.value=false
    }
    filesState[path]!!.value=is_checked
}
private val pagerSize=5000//100
private var videosSource: VideosPagingSource = VideosPagingSource()
private var audiosSource: AudiosPagingSource = AudiosPagingSource()
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
fun setContentResolver(contentResolver: ContentResolver){
        videosSource.setContentResolver(contentResolver)
        audiosSource.setContentResolver(contentResolver)
    }
val show_flag= mutableStateOf(0)
}