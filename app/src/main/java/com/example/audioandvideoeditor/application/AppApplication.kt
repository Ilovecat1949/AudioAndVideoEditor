package com.example.audioandvideoeditor.application

import TaskRepository
import android.app.Application
import com.example.audioandvideoeditor.dao.AppDatabase

class AppApplication : Application() {

    // 全局单例 Repository（唯一实例）
    lateinit var taskRepository: TaskRepository

    override fun onCreate() {
        super.onCreate()
        // 1. 初始化全局实例（第一行执行）
        INSTANCE = this
        // 1. 初始化 Room 数据库
        val database = AppDatabase.getDatabase(this)
        val tasksDao = database.taskDao()

        // 2. 创建全局 Repository（初始 Binder = null）
        taskRepository = TaskRepository(
            tasksDao = tasksDao,
            tasksBinder = null
        )
    }

    companion object {
        // 全局安全获取 Application 实例
        lateinit var INSTANCE: AppApplication
            private set
    }
}