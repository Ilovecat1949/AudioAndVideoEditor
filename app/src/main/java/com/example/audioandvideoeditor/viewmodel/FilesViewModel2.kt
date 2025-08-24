package com.example.audioandvideoeditor.viewmodel

import android.content.ContentResolver
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.example.audioandvideoeditor.dao.AudiosPagingSource
import com.example.audioandvideoeditor.dao.VideosPagingSource
import com.example.audioandvideoeditor.entity.AudioInfo
import com.example.audioandvideoeditor.entity.VideoInfo
import kotlinx.coroutines.sync.Mutex
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
    val displayFilesList: List<File> get() = filesList.sortedWith(currentComparator)
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

    var backHandler_flag by mutableStateOf(false)
    val mutex = Mutex()
    val thumbnailsMaxNum=100
    val thumbnailBitmapArray=ArrayList<Pair<String,Bitmap>>()
    // ViewModel被清除时调用此方法
    override fun onCleared() {
        super.onCleared()
        releaseBitmaps() // 在ViewModel清除时释放资源
    }
    private fun releaseBitmaps() {
        for (pair in thumbnailBitmapArray) {
            if (!pair.second.isRecycled) {
                pair.second.recycle()
            }
        }
        thumbnailBitmapArray.clear() // 清空列表
        //println("ViewModel中的所有Bitmap资源已释放并清空列表。")
    }
    fun setSortCriteriaAndOrder(flag:Int){
        sortCriteria=flag/3
        sortOrder=flag%3
    }
    var sortCriteria by mutableStateOf(0)
        private set
    var sortOrder by mutableStateOf(1)
        private set
    fun setFileSortCriteria(criteria: Int) {
        sortCriteria = criteria
    }
    fun toggleSortOrder() {
        sortOrder = if (sortOrder == 1) 0 else 1
    }
    // New Enum for Sort Criteria

    private val currentComparator: Comparator<File>
        get() = Comparator<File> { file1, file2 ->
            val result = when (sortCriteria) {
                0 -> file1.name.compareTo(file2.name, ignoreCase = true)
                1 -> file1.length().compareTo(file2.length())
                2 -> file1.lastModified() .compareTo(file2.lastModified())
                else -> file1.name.compareTo(file2.name, ignoreCase = true)
            }
            if (sortOrder == 0) -result else result
        }.let {
            // Always show directories first, regardless of other sorting criteria
            // This is a common and good UX practice for file managers
            Comparator<File> { file1, file2 ->
                when {
                    file1.isDirectory && !file2.isDirectory -> -1
                    !file1.isDirectory && file2.isDirectory -> 1
                    else -> 0
                }
            }.then(it) // Apply directory sorting first, then the chosen criteria
        }
}