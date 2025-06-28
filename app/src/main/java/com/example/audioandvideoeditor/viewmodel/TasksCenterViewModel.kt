package com.example.audioandvideoeditor.viewmodel

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.audioandvideoeditor.dao.TasksDao
import com.example.audioandvideoeditor.entity.Task
import com.example.audioandvideoeditor.entity.TaskInfo
import com.example.audioandvideoeditor.services.TasksBinder
import java.io.BufferedReader
import java.io.File
import kotlin.concurrent.thread

class TasksCenterViewModel: ViewModel() {
    private val TAG="TasksCenterViewModel"
    var tasks_binder_flag= mutableStateOf(false)
    lateinit var tasksBinder: TasksBinder
    lateinit var tasksDao: TasksDao
    lateinit var readContext:(file: File, route:String, flag:Boolean)->Unit
    val watingTasksList= mutableStateListOf<TaskInfo>()
    val runningTasksList= mutableStateListOf<TaskInfo>()
    val cancelledTasksList= mutableStateListOf<TaskInfo>()
    val endedTasksList= mutableStateListOf<TaskInfo>()
    val failedTasksList= mutableStateListOf<TaskInfo>()
    val tasksState=HashMap<Long,MutableState<String>>()
    fun reFresh(){
        if(refresh_flag){
            //runningTasks
            var list=tasksBinder.getRunningTasksQueue()
            var i=0
            while(i<runningTasksList.size){
                if(!list.contains(runningTasksList[i])){
                    runningTasksList.removeAt(i)
                }
                else{
                    i++
                }
            }
            i=0
            while(i<list.size){
                if(!runningTasksList.contains(list[i])){
                    runningTasksList.add(0,list[i])
                }
                i++
            }
            Log.d(TAG,"runningTasksList.size:${runningTasksList.size}")
            i=0
            while(i<runningTasksList.size){
                if(tasksState.containsKey(runningTasksList[i].long_arr[0])){
                    val progress=tasksBinder.getTaskProgress(runningTasksList[i].long_arr[0])
                    if(progress>0 && progress<1){
                        tasksState[runningTasksList[i].long_arr[0]]!!.value=String.format("%.2f",progress*100)+"%"
                    }
                    else if(progress<0){
                        tasksState[runningTasksList[i].long_arr[0]]!!.value="0%"
                    }
                    else if(progress>1){
                        tasksState[runningTasksList[i].long_arr[0]]!!.value="99.99%"
                    }
                }
                i++
            }
            //watingTasks
            list=tasksBinder.getWatingTasksQueue()
            i=0
            while(i<watingTasksList.size){
                if(!list.contains(watingTasksList[i])){
                    watingTasksList.removeAt(i)
                }
                else{
                    i++
                }
            }
            i=0
            while(i<list.size){
                if(!watingTasksList.contains(list[i])){
                    watingTasksList.add(0,list[i])
                }
                i++
            }
            //cancelledTasks
            list=tasksBinder.getTasksQueue()
            i=0
            while(i<list.size){
                if(tasksBinder.getTaskState(list[i].long_arr[0])==2){
                    if(!Contains(cancelledTasksList,list[i].long_arr[0])){
                        cancelledTasksList.add(0,list[i])
                    }
                }
                i++
            }
            //endedTasks
            i=0
            while(i<list.size){
                if(tasksBinder.getTaskState(list[i].long_arr[0])==1){
                    if(!Contains(endedTasksList,list[i].long_arr[0])){
                        endedTasksList.add(0,list[i])
                    }
                }
                i++
            }
            i=0
            while(i<list.size){
                if(tasksBinder.getTaskState(list[i].long_arr[0])==-1){
                    if(!Contains(failedTasksList,list[i].long_arr[0])){
                        failedTasksList.add(0,list[i])
                    }
                }
                i++
            }
        }
        else if(initlist_flag){
            initlist_flag=false
            thread {
                initList()
            }
        }
    }
    var refresh_flag=false
    var initlist_flag=true
    private fun initList(){
      runningTasksList.clear()
      runningTasksList.addAll(tasksBinder.getRunningTasksQueue())
      watingTasksList.clear()
      watingTasksList.addAll(tasksBinder.getWatingTasksQueue())
      cancelledTasksList.clear()
      endedTasksList.clear()
      failedTasksList.clear()
      cancelledTasksList.addAll(TasksToTaskInfos(tasksDao.loadTasksForStatus(2)))
      endedTasksList.addAll(TasksToTaskInfos(tasksDao.loadTasksForStatus(1)))
      failedTasksList.addAll(TasksToTaskInfos(tasksDao.loadTasksForStatus(-1)))
      refresh_flag=true
      initlist_flag=true
    }
    private fun TasksToTaskInfos(tasksList:List<Task>):List<TaskInfo>{
        val infosList=ArrayList<TaskInfo>()
        var i=0
        while(i<tasksList.size){
            val long_arr=ArrayList<Long>()
            long_arr.add(tasksList[i].task_id)
            val int_arr=ArrayList<Int>()
            int_arr.add(tasksList[i].type)
            val str_arr=ArrayList<String>()
            str_arr.add(tasksList[i].path)
            str_arr.add(tasksList[i].log_path)
            infosList.add(
                TaskInfo(
                    int_arr,
                    long_arr,
                    str_arr,
                    ArrayList()
                )
            )
            i++
        }
        return infosList
    }
    private fun Contains(infoslist:List<TaskInfo>,taskid:Long):Boolean{
        var i=0
        while(i<infoslist.size){
            if(infoslist[i].long_arr[0]==taskid){
                return  true
            }
            i++
        }
        return false
    }
    fun cancelTask(id:Long){
        tasksBinder.cancelTask(id)
    }
    val show_flag= mutableStateOf(0)
    val show_log_flag_map=HashMap<String,MutableState<Boolean>>()

    lateinit var file: File
    lateinit var bufferedReader: BufferedReader
    val log_lines= mutableStateListOf<String>()
    fun readLogFile(){
        var i=0
        var line:String?
        while(i<100){
            line=bufferedReader.readLine()
            if(line!=null){
                log_lines.add(line)
            }
            else{
                break
            }
            i++
        }
    }
}