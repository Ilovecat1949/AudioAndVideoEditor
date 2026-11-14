package com.example.audioandvideoeditor.ui.mediaconcat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.io.File

enum class ConcatMediaType {
    VIDEO,
    AUDIO
}

enum class ConcatEngineMode {
    FAST_DEMUXER,      // 极速无损 (concat demuxer)
    COMPATIBLE_FILTER  // 通用兼容 (concat filter)
}

class MediaConcatViewModel : ViewModel() {
    var mediaType by mutableStateOf(ConcatMediaType.VIDEO)
        private set

    var engineMode by mutableStateOf(ConcatEngineMode.FAST_DEMUXER)

    val fileList = mutableStateListOf<File>()

    var outputFileName by mutableStateOf("")

    // 分辨率策略选择状态
    var selectedResolutionIndex by mutableStateOf(0) // 0: Auto, 1: 1080p, 2: 720p, 3: Vertical

    val videoExtensionOptions = listOf("mp4", "mkv", "mov", "ts")
    val audioExtensionOptions = listOf("mp3", "m4a", "aac", "wav", "flac")

    var selectedExtension by mutableStateOf(videoExtensionOptions[0])

    val currentExtensionOptions: List<String>
        get() = if (mediaType == ConcatMediaType.VIDEO) videoExtensionOptions else audioExtensionOptions

    val canStartConcat: Boolean
        get() = fileList.size >= 2 && outputFileName.isNotBlank()

    fun switchMediaType(newType: ConcatMediaType) {
        if (mediaType != newType) {
            mediaType = newType
            selectedExtension = if (newType == ConcatMediaType.VIDEO) {
                videoExtensionOptions[0]
            } else {
                audioExtensionOptions[0]
            }
        }
    }

    fun appendPickedFiles(pickedFiles: List<File>?) {
        if (pickedFiles.isNullOrEmpty()) return
        pickedFiles.forEach { file ->
            if (file.exists() && fileList.none { it.absolutePath == file.absolutePath }) {
                fileList.add(file)
            }
        }
    }

    fun moveItemUp(index: Int) {
        if (index in 1 until fileList.size) {
            val item = fileList.removeAt(index)
            fileList.add(index - 1, item)
        }
    }

    fun moveItemDown(index: Int) {
        if (index in 0 until fileList.size - 1) {
            val item = fileList.removeAt(index)
            fileList.add(index + 1, item)
        }
    }

    fun removeFileAt(index: Int) {
        if (index in fileList.indices) {
            fileList.removeAt(index)
        }
    }

    fun clearAllFiles() {
        fileList.clear()
    }

    fun resetState() {
        fileList.clear()
        outputFileName = ""
    }
}