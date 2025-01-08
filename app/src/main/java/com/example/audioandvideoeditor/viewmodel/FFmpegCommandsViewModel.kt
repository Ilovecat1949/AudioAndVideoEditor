package com.example.audioandvideoeditor.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileReader


class FFmpegCommandsViewModel: ViewModel()  {
    val command_args_str= mutableStateOf("")
    val command_args= mutableStateListOf<MutableState<String>>()
    val show_log_flag= mutableStateOf(false)
    var read_log_flag=0
      private set
    var task_flag=0
    private lateinit var file: File
    private lateinit var bufferedReader: BufferedReader
    val log_lines= mutableStateListOf<String>()
    var task_log_path=""
    private fun readLogFile(){
        var i=0
        var line:String?
        while(i<10){
            line=bufferedReader.readLine()
            if(line!=null){
                if(log_lines.size>19){
                  for(j in 0..9){
                      log_lines.removeAt(j)
                    }
                }
                log_lines.add(line)
                if(line.contains("FFmpegCommandsTask END")){
                    bufferedReader.close()
                    read_log_flag=2
                    break
                }
            }
            else{
                break
            }
            i++
        }
    }
    fun startReadLogFile(){
        viewModelScope.launch{
            log_lines.clear()
            while(read_log_flag!=2 && this.isActive){
                if(read_log_flag==0) {
                    file=File(task_log_path)
                    if(file.exists()&&file.canRead()){
                        val reader = FileReader(file)
                        bufferedReader = BufferedReader(reader)
                        read_log_flag=1
                    }
                    else{
                        delay(500)
                        continue
                    }
                }else{
                    readLogFile()
                    delay(500)
                }
                }
            task_flag=0
            read_log_flag=0
        }
//        thread{
//            log_lines.clear()
//            while(read_log_flag!=2){
//                if(read_log_flag==0) {
//                    file=File(task_log_path)
//                    if(file.exists()&&file.canRead()){
//                        val reader = FileReader(file)
//                        bufferedReader = BufferedReader(reader)
//                        read_log_flag=1
//                    }
//                    else{
//                        sleep(100)
//                        continue
//                    }
//                }else{
//                    readLogFile()
//                    sleep(100)
//                }
//                }
//            task_flag=0
//            read_log_flag=0
//        }
    }
}