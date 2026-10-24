package com.example.audioandvideoeditor.viewmodel

import android.content.ContentResolver
import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.example.audioandvideoeditor.dao.AudiosPagingSource
import com.example.audioandvideoeditor.dao.VideosPagingSource
import kotlinx.coroutines.sync.Mutex
import java.io.File

class FilesViewModel : ViewModel() {
    val filesList = mutableStateListOf<File>()

    // 🌟 0 = 单选模式, 1 = 多选模式（带顺序编号）
    var selectionMode by mutableStateOf(0)

    // 🌟 严格保持用户选择先后顺序的响应式选中列表（SSOT）
    val selectedFiles = mutableStateListOf<File>()

    var parent: File? = null

    /**
     * 🌟 统一的文件勾选/取消切换逻辑（完美适配单选与多选）
     */
    fun toggleFileSelection(file: File) {
        val existingIndex = selectedFiles.indexOfFirst { it.path == file.path }
        if (selectionMode == 0) {
            // 单选模式：点击已选中则清空，未选中则替换为当前项
            if (existingIndex != -1) {
                selectedFiles.clear()
            } else {
                selectedFiles.clear()
                selectedFiles.add(file)
            }
        } else {
            // 多选模式：已存在则移除（后续元素自动顺延），不存在则追加到末尾
            if (existingIndex != -1) {
                selectedFiles.removeAt(existingIndex)
            } else {
                selectedFiles.add(file)
            }
        }
    }

    private val pagerSize = 5000
    private var videosSource: VideosPagingSource = VideosPagingSource()
    private var audiosSource: AudiosPagingSource = AudiosPagingSource()

    val videosPager = Pager(
        PagingConfig(pageSize = pagerSize, enablePlaceholders = true)
    ) {
        videosSource
    }

    val audiosPager = Pager(
        PagingConfig(pageSize = pagerSize, enablePlaceholders = true)
    ) {
        audiosSource
    }

    fun setContentResolver(contentResolver: ContentResolver) {
        videosSource.setContentResolver(contentResolver)
        audiosSource.setContentResolver(contentResolver)
    }

    val show_flag = mutableStateOf(0)
    var backHandler_flag by mutableStateOf(false)

    val mutex = Mutex()
    val thumbnailsMaxNum = 100
    val thumbnailBitmapArray = ArrayList<Pair<String, Bitmap>>()

    override fun onCleared() {
        super.onCleared()
        releaseBitmaps()
    }

    private fun releaseBitmaps() {
        for (pair in thumbnailBitmapArray) {
            if (!pair.second.isRecycled) {
                pair.second.recycle()
            }
        }
        thumbnailBitmapArray.clear()
    }

    val displayFilesList: List<File> get() = filesList.sortedWith(currentComparator)

    fun setSortCriteriaAndOrder(flag: Int) {
        sortCriteria = flag / 3
        sortOrder = flag % 3
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

    private val currentComparator: Comparator<File>
        get() = Comparator<File> { file1, file2 ->
            val result = when (sortCriteria) {
                0 -> file1.name.compareTo(file2.name, ignoreCase = true)
                1 -> file1.length().compareTo(file2.length())
                2 -> file1.lastModified().compareTo(file2.lastModified())
                else -> file1.name.compareTo(file2.name, ignoreCase = true)
            }
            if (sortOrder == 0) -result else result
        }.let {
            Comparator<File> { file1, file2 ->
                when {
                    file1.isDirectory && !file2.isDirectory -> -1
                    !file1.isDirectory && file2.isDirectory -> 1
                    else -> 0
                }
            }.then(it)
        }
}