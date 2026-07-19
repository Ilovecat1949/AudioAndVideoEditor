package com.example.audioandvideoeditor.ui.taskcenter

import TaskRepository
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audioandvideoeditor.application.AppApplication
import com.example.audioandvideoeditor.entity.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import androidx.core.net.toUri

class TasksCenterViewModel : ViewModel() {

    // ==================== 唯一依赖：Repository ====================
    private val repository: TaskRepository?
        get() = AppApplication.INSTANCE.taskRepository

    // ==================== Compose 响应式列表（状态自动同步） ====================
    /** 运行中任务 */
    val runningTasks = mutableStateListOf<Task>()
    /** 等待中任务 */
    val waitingTasks = mutableStateListOf<Task>()
    /** 历史任务（已完成/已取消/失败） */
    val historyTasks = mutableStateListOf<Task>()

    // ==================== 日志弹窗状态 ====================
    val showLogDialog = mutableStateOf(false)
    val logContent = mutableStateListOf<String>()

    // ==================== 自动刷新任务 ====================
    private var autoRefreshJob: Job? = null

    //  ==================== 删除任务 ====================
    var taskToDelete:Task?=null
    val showDeleteDialog= mutableStateOf(false)

    fun deleteTask(deleteFile: Boolean) {
        if(taskToDelete!=null) {
            viewModelScope.launch(Dispatchers.IO) {
                if (deleteFile) {
                    val context = AppApplication.INSTANCE.applicationContext
                    // 1. 优先通过 uri 删除（官方标准方式）
                    taskToDelete!!.uri.takeIf { it.isNotEmpty() }?.let { uriString ->
                        try {
                            val uri = uriString.toUri()
                            when (uri.scheme) {
                                "content" -> {
                                    // ✅ 谷歌官方标准 API：直接删除 content:// 资源
                                    context.contentResolver.delete(uri, null, null)
                                }

                                "file" -> {
                                    // file:// 协议走文件系统删除
                                    uri.path?.let { path ->
                                        File(path).takeIf { it.exists() }?.delete()
                                    }
                                }
                            }
                        } catch (e: SecurityException) {
                            // 可能没有该 Uri 的写入权限，忽略（仅删除记录）
                        } catch (e: Exception) {
                            // 其他异常忽略
                        }
                    }

                    // 2. 如果 uri 为空或删除失败，再尝试通过 path 删除（兜底）
                    //    注意：部分旧数据可能只有 path 没有 uri
                    taskToDelete!!.path.takeIf { it.isNotEmpty() }?.let { path ->
                        try {
                            File(path).takeIf { it.exists() }?.delete()
                        } catch (e: Exception) {
                            // 忽略
                        }
                    }
                    taskToDelete!!.log_path .takeIf { it.isNotEmpty() }?.let { path ->
                        try {
                            File(path).takeIf { it.exists() }?.delete()
                        } catch (e: Exception) {
                            // 忽略
                        }
                    }
                }
                // 3. 无论文件是否删除成功，都删除数据库记录
                repository?.deleteTaskById(taskToDelete!!.task_id)

                // 4. 刷新列表
                refreshAllTaskData()
                taskToDelete = null
            }
        }
    }
    // ==================== 初始化：启动自动刷新 ====================
    init {
        startTaskAutoRefresh()
    }

    // ==================== 【核心】自动刷新：处理状态切换 ====================
    /**
     * 每1秒拉取一次最新任务
     * 自动处理：运行中 → 完成/取消/失败
     * 列表自动更新，UI自动刷新
     */
    private fun startTaskAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                refreshAllTaskData()
                delay(1000) // 1秒刷新一次，兼顾性能与实时性
            }
        }
    }

    // ==================== 全量刷新任务（状态切换的核心） ====================
    private suspend fun refreshAllTaskData() {
        withContext(Dispatchers.IO) {
            // 1. 获取最新的 运行中任务
            val newRunning = repository?.getRunningTasks() ?: emptyList()
            // 2. 获取最新的 等待中任务
            val newWaiting = repository?.getWaitingTasks() ?: emptyList()
            // 3. 获取最新的 历史任务（完成/取消/失败）
            val newHistory = repository?.getHistoryTasks() ?: emptyList()

            // 切主线程更新UI列表（Compose 自动重组）
            withContext(Dispatchers.Main) {
                // 运行中任务：自动替换（完成的任务会自动消失）
                runningTasks.clear()
                runningTasks.addAll(newRunning)

                // 等待中任务同步
                waitingTasks.clear()
                waitingTasks.addAll(newWaiting)

                // 历史任务：新增的完成/取消任务自动加入
                historyTasks.clear()
                historyTasks.addAll(newHistory)
            }
        }
    }

    // ==================== 手动操作 ====================
    /** 取消任务 */
    fun cancelTask(taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository?.cancelTask(taskId)
            refreshAllTaskData() // 取消后立即刷新
        }
    }

    /** 获取任务进度 */
    fun getTaskProgress(taskId: Long): Float {
        return repository?.getTaskProgress(taskId) ?: 0f
    }

    // ==================== 日志功能（安全无泄漏） ====================


    fun closeLog() {
        showLogDialog.value = false
//        logContent.clear()
    }

    // ==================== 页面销毁：清理 ====================
    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel() // 停止刷新，防泄漏
        logContent.clear()
    }
}