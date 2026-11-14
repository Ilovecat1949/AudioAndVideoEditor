package com.example.audioandvideoeditor.viewmodel

import android.content.ContentResolver
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.example.audioandvideoeditor.dao.AudiosPagingSource
import com.example.audioandvideoeditor.dao.ImagesPagingSource
import com.example.audioandvideoeditor.dao.VideosPagingSource
import com.example.audioandvideoeditor.entity.AudioInfo
import com.example.audioandvideoeditor.entity.ImageInfo
import com.example.audioandvideoeditor.entity.VideoInfo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import java.io.File

class FilesViewModel2 : ViewModel()  {
    private val pagerSize=5000//100
    private var videosSource: VideosPagingSource = VideosPagingSource()
    private var audiosSource: AudiosPagingSource = AudiosPagingSource()

    private var imagesSource: ImagesPagingSource = ImagesPagingSource() // 🌟 新增
    fun setContentResolver(contentResolver: ContentResolver){
        videosSource.setContentResolver(contentResolver)
        audiosSource.setContentResolver(contentResolver)
        imagesSource.setContentResolver(contentResolver) // 🌟 新增
    }
    val show_flag= mutableStateOf(0)
    val show_details_flag= mutableStateOf(0)
    val  filesList= mutableStateListOf<File>()
    val displayFilesList: List<File> get() = filesList.sortedWith(currentComparator)
    var file:File?=null
    var parent: File?=null
    var audioInfo: AudioInfo?=null
    var videoInfo: VideoInfo?=null
    var imageInfo: ImageInfo?=null // 🌟 新增
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

    val imagesPager = Pager( // 🌟 新增
        PagingConfig(pageSize = pagerSize, enablePlaceholders = true)
    ) {
        imagesSource
    }

    // 🌟 新增：图片加载并发限制（同时最多允许 3 个解码任务）
    val imageLoadSemaphore = Semaphore(3)

    // 🌟 新增：LruCache 内存缓存
    val imageMemoryCache = object : LruCache<String, Bitmap>(
        // 分配最大可用内存的 1/8 作为缓存
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            // 返回 Bitmap 占用的字节数（KB），LruCache 据此计算总大小
            return value.byteCount / 1024
        }

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            // 🌟 关键：不手动 recycle，只从缓存中移除引用，交给 GC 回收。
            // 避免图片还在 Compose 画布上渲染时被强行回收导致崩溃。
        }
    }

    // 方便 UI 层安全调用
    fun getImageFromCache(path: String): Bitmap? = imageMemoryCache.get(path)
    fun putImageToCache(path: String, bitmap: Bitmap) {
        imageMemoryCache.put(path, bitmap)
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