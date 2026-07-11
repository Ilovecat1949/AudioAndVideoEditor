package com.example.audioandvideoeditor.ui.avinfo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audioandvideoeditor.application.AppApplication
import com.example.audioandvideoeditor.entity.MediaInfo
import com.example.audioandvideoeditor.services.TasksBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudioAndVideoInfoViewModel: ViewModel()  {

    private var info_text= ""
    // 🌟 核心微调：使用 Compose 标准的 State 包裹 MediaInfo 对象
    // 这样只要 info 发生了赋值，Compose 会瞬间捕捉到并精准刷新 UI
    var info by mutableStateOf(MediaInfo())
        private set
    fun getInfo(path:String){
        viewModelScope.launch {
            setInfoText(path)
        }
    }
    private suspend fun setInfoText(path:String) = withContext(Dispatchers.Default) {
        val text = AppApplication.INSTANCE.taskRepository.getAVInfo(path) //

        // 🌟 线程安全与响应式改造：在 Default 线程计算好新对象，然后切回主线程或者利用声明式直接感知
        val newInfo = MediaInfo()
        newInfo.initInfo(text)

        // 回到主线程（或在特定安全机制下）整体替换状态引用，一触即发驱动前台重组
        withContext(Dispatchers.Main) {
            info_text = text
            info = newInfo // 👈 这一行赋值会直接引爆前台 UI 的自动全量刷新！
        }
        println(info_text)
    }
}