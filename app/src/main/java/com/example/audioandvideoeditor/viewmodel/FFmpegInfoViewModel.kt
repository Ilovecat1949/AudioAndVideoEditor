package com.example.audioandvideoeditor.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.audioandvideoeditor.services.TasksBinder

class FFmpegInfoViewModel: ViewModel() {
    lateinit var tasksBinder: TasksBinder
    val show_flag= mutableStateOf(0)
    var tasks_binder_flag= mutableStateOf(false)
}