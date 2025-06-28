package com.example.audioandvideoeditor.viewmodel

import android.app.Activity
import android.os.Environment
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audioandvideoeditor.MainActivity
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.utils.ConfigsUtils
import kotlinx.coroutines.launch
import java.io.File

class ConfigViewModel: ViewModel()  {
    val sizeForVideoEncodingTaskText= mutableStateOf("")
    val editSizeForVideoEncodingTaskFlag=mutableStateOf(false)
    val checkSizeForVideoEncodingTaskFlag=mutableStateOf(0)
    val sizeForAudioEncodingTaskText= mutableStateOf("")
    val editSizeForAudioEncodingTaskFlag=mutableStateOf(false)
    val checkSizeForAudioEncodingTaskFlag=mutableStateOf(0)
    val sizeForMaxTasksNumText= mutableStateOf("")
    val editSizeForMaxTasksNumFlag=mutableStateOf(false)
    val checkSizeForMaxTasksNumFlag=mutableStateOf(0)
    val LanguageText= mutableStateOf("")
    val editLanguageFlag=mutableStateOf(false)
    val checkLanguageFlag=mutableStateOf(0)

    val showPathDialog= mutableStateOf(false)
    private val _downloadPath = mutableStateOf("")
    val downloadPath: State<String> = _downloadPath

    fun initConfig(activity: MainActivity){
        when(ConfigsUtils.sizeForVideoEncodingTask){
            ConfigsUtils.AV_10MB->{sizeForVideoEncodingTaskText.value="10MB"
                checkSizeForVideoEncodingTaskFlag.value=0
            }
            ConfigsUtils.AV_50MB->{sizeForVideoEncodingTaskText.value="50MB"
                checkSizeForVideoEncodingTaskFlag.value=1
            }
            ConfigsUtils.AV_100MB->{sizeForVideoEncodingTaskText.value="100MB"
                checkSizeForVideoEncodingTaskFlag.value=2
            }
            ConfigsUtils.AV_500MB->{sizeForVideoEncodingTaskText.value="500MB"
                checkSizeForVideoEncodingTaskFlag.value=3
            }
            ConfigsUtils.AV_1GB->{sizeForVideoEncodingTaskText.value="1GB"
                checkSizeForVideoEncodingTaskFlag.value=4
            }
            ConfigsUtils.AV_2GB->{sizeForVideoEncodingTaskText.value="2GB"
                checkSizeForVideoEncodingTaskFlag.value=5
            }
            ConfigsUtils.AV_3GB->{sizeForVideoEncodingTaskText.value="3GB"
                checkSizeForVideoEncodingTaskFlag.value=6
            }
        }
        when(ConfigsUtils.sizeForAudioEncodingTask){
            ConfigsUtils.AV_10MB->{sizeForAudioEncodingTaskText.value="10MB"
                checkSizeForAudioEncodingTaskFlag.value=0
            }
            ConfigsUtils.AV_50MB->{sizeForAudioEncodingTaskText.value="50MB"
                checkSizeForAudioEncodingTaskFlag.value=1
            }
            ConfigsUtils.AV_100MB->{sizeForAudioEncodingTaskText.value="100MB"
                checkSizeForAudioEncodingTaskFlag.value=2
            }
            ConfigsUtils.AV_500MB->{sizeForAudioEncodingTaskText.value="500MB"
                checkSizeForAudioEncodingTaskFlag.value=3
            }
            ConfigsUtils.AV_1GB->{sizeForAudioEncodingTaskText.value="1GB"
                checkSizeForAudioEncodingTaskFlag.value=4
            }
            ConfigsUtils.AV_2GB->{sizeForAudioEncodingTaskText.value="2GB"
                checkSizeForAudioEncodingTaskFlag.value=5
            }
            ConfigsUtils.AV_3GB->{sizeForAudioEncodingTaskText.value="3GB"
                checkSizeForAudioEncodingTaskFlag.value=6
            }
        }
        sizeForMaxTasksNumText.value=ConfigsUtils.MAX_TASKS_NUM.toString()
        checkSizeForMaxTasksNumFlag.value=ConfigsUtils.MAX_TASKS_NUM-2
        when(ConfigsUtils.language){
            ConfigsUtils.English->{
                LanguageText.value=activity.resources.getString(R.string.english)
                checkLanguageFlag.value=1
            }
            else->{
                LanguageText.value=activity.resources.getString(R.string.simplified_chinese)
                checkLanguageFlag.value=0
            }
        }
        _downloadPath.value=ConfigsUtils.target_dir
    }

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    fun updateDownloadPath(newPath: String,activity:Activity) {
        viewModelScope.launch {
            try {
                val file = File(newPath)
                if (!file.exists() && !file.mkdirs()) {
                    throw Exception("Failed to create directory.")
                }
                _downloadPath.value = newPath

                ConfigsUtils.setTargetDir(newPath,activity)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error"
            }
        }
    }
    fun setSizeForVideoEncodingTask(activity: Activity){
        var size=0L
        when(checkSizeForVideoEncodingTaskFlag.value){
            0->{size=ConfigsUtils.AV_10MB
                sizeForVideoEncodingTaskText.value="10MB"
            }
            1->{size=ConfigsUtils.AV_50MB
                sizeForVideoEncodingTaskText.value="50MB"
            }
            2->{size=ConfigsUtils.AV_100MB
                sizeForVideoEncodingTaskText.value="100MB"
            }
            3->{size=ConfigsUtils.AV_500MB
                sizeForVideoEncodingTaskText.value="500MB"
            }
            4->{size=ConfigsUtils.AV_1GB
                sizeForVideoEncodingTaskText.value="1GB"
            }
            5->{size=ConfigsUtils.AV_2GB
                sizeForVideoEncodingTaskText.value="2GB"
            }
            6->{size=ConfigsUtils.AV_3GB
                sizeForVideoEncodingTaskText.value="3GB"
            }
        }
        ConfigsUtils.setSizeForVideoEncodingTask(size,activity)
    }
    fun setSizeForAudioEncodingTask(activity: Activity){
        var size=0L
        when(checkSizeForAudioEncodingTaskFlag.value){
            0->{size=ConfigsUtils.AV_10MB
                sizeForAudioEncodingTaskText.value="10MB"
            }
            1->{size=ConfigsUtils.AV_50MB
                sizeForAudioEncodingTaskText.value="50MB"
            }
            2->{size=ConfigsUtils.AV_100MB
                sizeForAudioEncodingTaskText.value="100MB"
            }
            3->{size=ConfigsUtils.AV_500MB
                sizeForAudioEncodingTaskText.value="500MB"
            }
            4->{size=ConfigsUtils.AV_1GB
                sizeForAudioEncodingTaskText.value="1GB"
            }
            5->{size=ConfigsUtils.AV_2GB
                sizeForAudioEncodingTaskText.value="2GB"
            }
            6->{size=ConfigsUtils.AV_3GB
                sizeForAudioEncodingTaskText.value="3GB"
            }
        }
        ConfigsUtils.setSizeForAudioEncodingTask(size,activity)
    }
    fun setMaxTasksNum(activity: Activity){
        sizeForMaxTasksNumText.value=(checkSizeForMaxTasksNumFlag.value+2).toString()
        ConfigsUtils.setMaxTasksNum(checkSizeForMaxTasksNumFlag.value+2,activity)
    }
    fun setLanguage(activity: MainActivity){
        when(checkLanguageFlag.value){
            1->{
                ConfigsUtils.setLanguage(ConfigsUtils.English,activity)
                LanguageText.value=activity.resources.getString(R.string.english)
            }
            else->{
                ConfigsUtils.setLanguage(ConfigsUtils.Simplified_Chinese,activity)
                LanguageText.value=activity.resources.getString(R.string.simplified_chinese)
            }
        }
        activity.setCurrLanguageMode()
    }
    var showClearFFmpegLogFilesDialogFlag = mutableStateOf(false)
    var showUpdateDialogFlag = mutableStateOf(false)
}