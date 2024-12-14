package com.example.audioandvideoeditor.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.audioandvideoeditor.entity.MediaInfo

class ReEncodingViewModel:ViewModel() {
    var input_file_name= mutableStateOf("")
    val options = listOf("mp4", "mkv", "m4a")
    val expanded =mutableStateOf(false)
    val selectedOptionText = mutableStateOf(options[0])
    val mediaInfo=MediaInfo()
    val editBitRateFlag= mutableStateOf(false)
    val bit_rate_text= mutableStateOf("")
    var bit_rate= -1L
    val editFrameRateFlag= mutableStateOf(false)
    val frame_rate_text= mutableStateOf("")
    var frame_rate= -1
}