package com.example.audioandvideoeditor.viewmodel

import androidx.lifecycle.ViewModel
import java.io.File

class HomeViewModel  : ViewModel()  {
    var file:File?=null
    var nextDestination:()->Unit={}
}