package com.example.audioandvideoeditor.ui.ffmpegcommand

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class FFmpegCommandsViewModel: ViewModel()  {
    val command_args_str= mutableStateOf("")
    val command_args= mutableStateListOf<MutableState<String>>()
    val show_log_flag= mutableStateOf(false)
    var task_flag=0
    val log_lines= mutableStateListOf<String>()
    var task_log_path=""

    var input_file by mutableStateOf("")
    var output_file_name by mutableStateOf("")
    val extensionTemplate= listOf("mp4","mkv","mov","flv","ts","avi","mp3","m4a")
    var extensionText by mutableStateOf(extensionTemplate[0])
    var parameterTemplateName= listOf<String>()
    val parameterTemplateContext=
        listOf(
            "-c:v libx264 -q:v 5 -c:a aac -ab 128k -ar 44100",
            "-c:v copy -c:a aac -ab 128k -ar 44100",
            "-vf scale=1280:720",
            "-b:v 500k",
            "-b:a 128k",
            "-r 45",
            "-ar 48000",
            "-ss 00:02:00 -t 00:03:00",
            "-vf crop=3/5*iw:ih:iw/5:0",
//            "-vf scale=1280:720",
            "-vn",
            "-c:v copy -an",
        )
    var parameterNameText by mutableStateOf("")
    var parameterContextText by mutableStateOf(parameterTemplateContext[0])
}