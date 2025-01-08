package com.example.audioandvideoeditor.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audioandvideoeditor.entity.MediaInfo
import com.example.audioandvideoeditor.services.TasksBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudioAndVideoInfoViewModel: ViewModel()  {
    var tasks_binder_flag= mutableStateOf(false)
    val info_text= mutableStateOf("")
    val info=MediaInfo()
    lateinit var tasksBinder: TasksBinder
    fun getInfo(path:String){
        viewModelScope.launch {
            setInfoText(path)
        }
    }
    private suspend fun setInfoText(path:String) = withContext(Dispatchers.Default) {
        val text=tasksBinder.getAVInfo(path)
        info_text.value=text
        info.initInfo(text)
    }
}