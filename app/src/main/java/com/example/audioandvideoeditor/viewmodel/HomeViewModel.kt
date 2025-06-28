package com.example.audioandvideoeditor.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.io.File

class HomeViewModel  : ViewModel()  {
    var file:File?=null
    var nextDestination:()->Unit={}
    var route_flag=false
    var show_on_screen_ad by mutableStateOf(true)
    var show_interstistial_ad by mutableStateOf(false)
    var show_crash_message_flag by mutableStateOf(false)
}