package com.example.audioandvideoeditor.ui.avmerge

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.io.File

enum class MergePickingTarget {
    NONE,
    VIDEO,
    AUDIO
}

class AudioVideoMergeViewModel : ViewModel() {
    // 核心输入文件状态
    var selectedVideoFile by mutableStateOf<File?>(null)
        private set

    var selectedAudioFile by mutableStateOf<File?>(null)
        private set

    // 记录当前点击了哪个槽位去挑文件，方便从文件选择器返回时精准回填
    var currentPickingTarget by mutableStateOf(MergePickingTarget.NONE)

    // 合并参数选项（默认开启自动对齐时长、默认替换原声）
    var isShortestEnabled by mutableStateOf(true)
    var isReplaceOriginalAudio by mutableStateOf(true)

    // 输出配置
    var outputFileName by mutableStateOf("")
    val extensionOptions = listOf("mp4", "mkv", "mov")
    var selectedExtension by mutableStateOf(extensionOptions[0])

    // 衍生状态（SSOT）：只有同时选齐了视频、音频且文件名非空才允许提交
    val canStartMerge: Boolean
        get() = selectedVideoFile != null && selectedAudioFile != null && outputFileName.isNotBlank()

    /**
     * 处理从文件选择器返回的文件
     */
    fun onFilePicked(files: File) {
        val pickedFile = files
        when (currentPickingTarget) {
            MergePickingTarget.VIDEO -> selectedVideoFile = pickedFile
            MergePickingTarget.AUDIO -> selectedAudioFile = pickedFile
            MergePickingTarget.NONE -> Unit
        }
        // 重置选择目标，避免重组时重复消费
        currentPickingTarget = MergePickingTarget.NONE
    }

    fun clearVideoFile() {
        selectedVideoFile = null
    }

    fun clearAudioFile() {
        selectedAudioFile = null
    }

    fun resetState() {
        selectedVideoFile = null
        selectedAudioFile = null
        outputFileName = ""
        currentPickingTarget = MergePickingTarget.NONE
    }
}