package com.example.audioandvideoeditor.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class RePackagingViewModel: ViewModel()  {
    var input_file_name= mutableStateOf("")
    val options = listOf("mp4", "mkv", "m4a")
    val expanded = mutableStateOf(false)
    val selectedOptionText = mutableStateOf(options[0])
    val start_time_text=mutableStateOf("")
    var start_time=-1L
    val end_time_text=mutableStateOf("")
    var end_time=-1L
}