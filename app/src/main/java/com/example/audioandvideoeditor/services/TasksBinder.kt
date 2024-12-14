package com.example.audioandvideoeditor.services

import android.os.Binder
import com.example.audioandvideoeditor.dao.TasksDao
import com.example.audioandvideoeditor.entity.TaskInfo

class TasksBinder (service: TasksService) : Binder(){
    private var service: TasksService
    init{
        this.service=service
    }
//    fun initTasksDao(dao: TasksDao){
//        service.initTasksDao(dao)
//    }
    fun startTask(info: TaskInfo){
        service.startTask(info)
    }
    fun getAVInfo(path:String):String{
       return service.getAVStrInfo(path)
    }
    fun getRemainingTasksNum():Int{
        return service.getRemainingTasksNum()
    }
    fun getTaskState(id:Long):Int{
        return service.getTaskState(id)
    }
    fun getTasksQueue():List<TaskInfo>{
        return service.getTasksQueue()
    }
    fun getWatingTasksQueue():List<TaskInfo>{
        return service.getWatingTasksQueue()
    }
    fun getRunningTasksQueue():List<TaskInfo>{
        return service.getRunningTasksQueue()
    }
    fun getTaskProgress(id:Long):Float{
        return service.getTaskProgress(id)
    }
    fun cancelTask(id: Long){
        service.cancelTask(id)
    }
    fun getFFmpegInfo(info_type:Int):String{
        return service.getFFmpegInfo(info_type)
    }
}