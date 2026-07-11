package com.example.audioandvideoeditor.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.audioandvideoeditor.IFFmpegService
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.application.AppApplication
import com.example.audioandvideoeditor.entity.TaskInfo
import com.example.audioandvideoeditor.navigation.Destination
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.utils.FilesUtils
import com.example.audioandvideoeditor.utils.TextsUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Thread.sleep
import java.util.LinkedList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

/**
 * 任务管理服务：负责任务队列调度、JNI/FFmpeg任务生命周期管理、状态监控、通知展示
 * 核心能力：任务入队/取消/状态查询、进度监控、数据库持久化、通知更新
 */
class TaskService : Service() {
    // 日志TAG（固定值，避免魔法值）
    private val TAG = "TaskService"

    // 🌟 1. 统一收拢 Channel ID，彻底拒绝魔法值错位
    private val CHANNEL_ID = "TaskService_Channel"
    // JNI任务工厂句柄
    private var tasksFactoryHandle: Long = -1

    // 任务ID自增器
//    private var taskIdGenerator: Long = 0L

    // 最大并行任务数（从配置读取）
    private var maxConcurrentTasks: Int = 1

    // Binder对象（供外部绑定通信）
    private val mBinder = TasksBinder(this)

    // 数据库DAO（任务持久化）
//    private lateinit var tasksDao: TasksDao

    // FFmpeg服务绑定标记
    private var isFFmpegServiceBound = false

    // FFmpeg服务AIDL接口
    private var ffmpegService: IFFmpegService? = null

    // 新增：服务绑定超时时间（ms）
    private val SERVICE_BIND_TIMEOUT = 3000L
    // 新增：服务重连标记（避免重复重连）
    private val reConnectLock = ReentrantLock()
    private var isReconnecting = false

    // 通知管理器（懒加载，避免提前初始化）
    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    // ==================== 队列与缓存（线程安全） ====================
    // 等待执行的任务队列
    private val waitingTasksQueue = LinkedList<TaskInfo>()
    // 正在执行的任务队列
    private val runningTasksQueue = LinkedList<TaskInfo>()
    // 待取消的任务ID队列（批量处理）
    private val pendingCancelTaskIds = LinkedList<Long>()
    // 所有任务的总队列（记录全量任务）
    private val allTasksQueue = LinkedList<TaskInfo>()
    // 任务状态缓存（key：任务ID，value：状态码 0-运行中 1-完成 2-取消 -1-失败）
    private val taskStateCache = HashMap<Long, Int>()
    // 任务通知缓存（key：任务ID，value：通知构建器）
    private val taskNotificationCache = HashMap<Long, NotificationCompat.Builder>()
    // 任务进度缓存（key：任务ID，value：进度0-1）
    private val taskProgressCache = HashMap<Long, Float>()

    // 线程安全锁（保护队列/缓存操作）
    private val lock = ReentrantLock()
    // FFmpeg服务连接等待器
    private var serviceConnectedLatch: CountDownLatch? = null
    // 任务监控线程运行标记
    private var isMonitorRunning = false

    // ==================== FFmpeg服务连接回调 ====================
    private val ffmpegServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            ffmpegService = IFFmpegService.Stub.asInterface(service)
            serviceConnectedLatch?.countDown()
            Log.d(TAG, "FFmpegService 绑定成功")
            // 重置重连标记
            reConnectLock.lock()
            isReconnecting = false
            reConnectLock.unlock()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Log.e(TAG, "FFmpegService 意外断开连接")
            ffmpegService = null
            isFFmpegServiceBound = false
            // 新增：自动重连
            reConnectFFmpegService()
        }
    }

    // 顶部新增常量
    private val MAX_RECONNECT_TIMES = 3 // 最大重连3次
    private var reconnectCount = 0     // 当前重连次数

    // 替换重连方法（无递归、有限次数、彻底杜绝无限循环）
    private fun reConnectFFmpegService() {
        reConnectLock.lock()
        if (isReconnecting || reconnectCount >= MAX_RECONNECT_TIMES) {
            reConnectLock.unlock()
            return
        }
        isReconnecting = true
        reconnectCount++
        reConnectLock.unlock()

        thread(name = "FFmpegServiceReconnect", isDaemon = true) {
            try {
                sleep(1000)
                Log.d(TAG, "FFmpegService 第 $reconnectCount 次重连...")

                val intent = Intent(this@TaskService, FFmpegService::class.java)
                // 独立创建新Latch
                serviceConnectedLatch = CountDownLatch(1)
                // 🌟 修改为以下组合 Flags：
                val bindFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ 特权：直接将主进程的前台保活能力（Foreground Service Capabilities）平移传递给远端子进程
                            BIND_AUTO_CREATE or
                            BIND_IMPORTANT or
                            BIND_INCLUDE_CAPABILITIES
                } else {
                    BIND_AUTO_CREATE or BIND_IMPORTANT
                }
                val bindSuccess = bindService(intent, ffmpegServiceConnection, bindFlags)

                if (!bindSuccess) {
                    Log.e(TAG, "FFmpegService 重连绑定失败")
                    resetReconnectStatus()
                    return@thread
                }

                isFFmpegServiceBound = true
                val connected = serviceConnectedLatch?.await(SERVICE_BIND_TIMEOUT, TimeUnit.MILLISECONDS)
                if (connected == true) {
                    Log.d(TAG, "FFmpegService 重连成功")
                    reconnectCount = 0
                    resetReconnectStatus()
                } else {
                    Log.e(TAG, "FFmpegService 重连超时，剩余重试次数：${MAX_RECONNECT_TIMES - reconnectCount}")
                    resetReconnectStatus()
                    // 次数未耗尽则继续
                    if (reconnectCount < MAX_RECONNECT_TIMES) {
                        reConnectFFmpegService()
                    } else {
                        Log.e(TAG, "FFmpegService 重连次数耗尽，放弃重连")
                        reconnectCount = 0
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "FFmpegService 重连异常", e)
                resetReconnectStatus()
            }
        }
    }

    // 新增：重置重连状态
    private fun resetReconnectStatus() {
        reConnectLock.lock()
        isReconnecting = false
        reConnectLock.unlock()
    }
    // ==================== 生命周期 ====================
    override fun onCreate() {
        super.onCreate()
        try {
            // 初始化JNI任务工厂
            tasksFactoryHandle = initTasksFactory()

            // 初始化通知渠道（Android O+）
            initNotificationChannel()

            // 初始化数据库DAO
//            tasksDao = AppDatabase.getDatabase(this).taskDao()

            // 初始化任务ID生成器（线程安全）
//            initTaskIdGenerator()

            // 读取最大并行任务数配置
            maxConcurrentTasks = ConfigsUtils.MAX_TASKS_NUM

            // 新增：提前绑定FFmpeg服务（核心优化）
            bindFFmpegServiceInAdvance()

            Log.d(TAG, "TasksService 创建成功 | 最大并行任务数：$maxConcurrentTasks")
        } catch (e: Exception) {
            Log.e(TAG, "TasksService 创建失败", e)
        }
//        val CHANNEL_ID="1001"
//        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
//            .setContentTitle("正在准备下载...")
//            .setContentText("0%")
//            .setSmallIcon(R.drawable.movie_edit_24px) // 必须设置小图标，否则直接闪退
//            .setPriority(NotificationCompat.PRIORITY_LOW)
//            .setOngoing(true)       // 🌟 核心：设为 ongoing，用户无法在通知栏右滑删除它
//            .setSilent(true)        // 🌟 核心：静音，防止每更新一次进度就叮咚响一声
//            .setProgress(100, 0, false) // 初始化进度条
//            .build()
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "TasksService 销毁中...")
        try {
            // 停止任务监控线程
            isMonitorRunning = false

            // 清理所有运行中/等待中任务
            cleanAllTasks()

            // 解绑FFmpeg服务
            unbindFFmpegService()
        } catch (e: Exception) {
            Log.e(TAG, "TasksService 销毁异常", e)
        }
        Log.d(TAG, "TasksService 销毁完成")
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(ConfigsUtils.setCurrLanguageMode(newBase))
    }


    // 新增：提前绑定FFmpeg服务的方法
// 纯异步绑定，每次创建独立的 CountDownLatch，不复用
    private fun bindFFmpegServiceInAdvance() {
        if (isFFmpegServiceBound || ffmpegService != null) {
            return
        }
        try {
            val intent = Intent(this, FFmpegService::class.java)
            // 关键：每次绑定都新建 Latch，绝对不复用
            serviceConnectedLatch = CountDownLatch(1)
            val bindSuccess = bindService(intent, ffmpegServiceConnection, Context.BIND_AUTO_CREATE)
            if (bindSuccess) {
                isFFmpegServiceBound = true
                Log.d(TAG, "FFmpegService 异步绑定请求已发送")
            } else {
                Log.e(TAG, "FFmpegService 绑定请求发送失败")
                isFFmpegServiceBound = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "FFmpegService 绑定异常", e)
            isFFmpegServiceBound = false
        }
    }
    // ==================== 核心方法（优化后） ====================
    /**
     * 启动新任务（入队+触发监控）
     * @param info 任务信息
     */
    fun startTask(info: TaskInfo) {
        lock.lock()
        try {
            // ============= 核心修改：删除所有ID生成逻辑 =============
            // 直接使用外部(Repository)传入的带ID的TaskInfo
            waitingTasksQueue.add(info)
            allTasksQueue.add(info)

            if (!isMonitorRunning) {
                isMonitorRunning = true
                // 守护线程，防止泄漏
                thread(name = "TaskMonitor", isDaemon = true) {
                    startTaskMonitor()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "任务入队失败", e)
        } finally {
            lock.unlock()
        }
    }
    /**
     * 取消任务（线程安全，批量处理）
     * @param taskId 任务ID
     */
    fun cancelTask(taskId: Long) {
        lock.lock()
        try {
            pendingCancelTaskIds.add(taskId)
            Log.d(TAG, "标记任务取消 | 任务ID：$taskId")
        } catch (e: Exception) {
            Log.e(TAG, "标记任务取消失败 | 任务ID：$taskId", e)
        } finally {
            lock.unlock()
        }
    }

    /**
     * 获取任务状态
     * @param taskId 任务ID
     * @return 状态码：0-运行中 1-完成 2-取消 -1-失败 -2-不存在
     */
    fun getTaskState(taskId: Long): Int {
        return taskStateCache[taskId] ?: -2
    }

    /**
     * 获取任务进度（0-1）
     * @param taskId 任务ID
     * @return 进度值，不存在返回0
     */
    fun getTaskProgress(taskId: Long): Float {
        return taskProgressCache[taskId] ?: 0f
    }

    /**
     * 获取剩余任务数（等待+运行）
     */
    fun getRemainingTasksNum(): Int {
        lock.lock()
        val num = try {
            waitingTasksQueue.size + runningTasksQueue.size
        } finally {
            lock.unlock()
        }
        return num
    }

    // ==================== 内部工具方法（优化后） ====================
    /**
     * 初始化通知渠道（Android O+）
     */
    private fun initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, // 渠道ID更语义化
                this@TaskService.getString(R.string.notification_channel_task), // 渠道名（建议抽字符串资源）
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun createTaskNotification(taskInfo: TaskInfo, taskId: Long) {
//        if (!checkNotificationsPermission(this)) return

// 1. 创建指向 MainActivity 的 Intent
        val intent = Intent(this, Class.forName("com.example.audioandvideoeditor.MainActivity")).apply {
            // 让 Activity 复用，防止重复创建多个 MainActivity 实例
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // 🌟 核心：塞入你想让 Compose 识别的路由或动作标记
            putExtra("TARGET_ROUTE", Destination.TasksCenter.route)
            putExtra("TASK_ID", taskId)
        }

        // 2. 包装成 PendingIntent
        // 注意：Android 12+ (API 31+) 必须显式指定 PendingIntent.FLAG_IMMUTABLE 或 FLAG_UPDATE_CURRENT
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, taskId.toInt(), intent, flags)

        val taskPath = taskInfo.str_arr[0]
        val taskName = FilesUtils.getNameFromPath(taskPath)
        val notificationBuilder = if (taskInfo.int_arr[0] != 2) {
            // 普通任务通知
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(taskName)
                .setContentText("0%")
                .setProgress(100, 0, false)
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setSmallIcon(R.drawable.movie_edit_24px)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent) // 🌟 核心：设置点击动作
                .setSilent(true)
        } else {
            // FFmpeg任务通知（裁剪文件名后缀）
            val fileName = taskName.substring(0, taskName.length - 4)
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(fileName)
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setSmallIcon(R.drawable.movie_edit_24px)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent) // 🌟 核心：设置点击动作
                .setSilent(true)
        }

        notificationManager.notify(taskId.toInt(), notificationBuilder.build())
        taskNotificationCache[taskId] = notificationBuilder
    }


    /**
     * 初始化任务ID生成器（从数据库读取最大ID，线程安全）
     */
//    private fun initTaskIdGenerator() {
//        thread(name = "TaskIdGenerator") {
//            try {
//                val maxId = tasksDao.getMaxTaskId()
//                taskIdGenerator = maxId?.plus(1) ?: 0L
//                Log.d(TAG, "任务ID生成器初始化完成 | 起始ID：$taskIdGenerator")
//            } catch (e: Exception) {
//                Log.e(TAG, "任务ID生成器初始化失败，使用默认ID：0", e)
//                taskIdGenerator = 0L
//            }
//        }
//    }

    /**
     * 启动任务监控线程（核心调度逻辑）
     */
    private fun startTaskMonitor() {
        thread(name = "TaskMonitor") {
            Log.d(TAG, "任务监控线程启动")
            taskNotificationCache.clear()

            while (isMonitorRunning) {
                try {
                    // 1. 批量处理待取消的任务
                    processPendingCancelTasks()

                    // 2. 更新运行中任务的状态/进度
                    updateRunningTasksStatus()

                    // 3. 调度等待队列的任务（填充空闲槽位）
                    dispatchWaitingTasks()

                    // 4. 无任务时退出监控
                    if (waitingTasksQueue.isEmpty() && runningTasksQueue.isEmpty()) {
                        break
                    }

                    // 等待FFmpeg服务连接（如有）
                    serviceConnectedLatch?.await()

                    // 降低轮询频率，减少CPU消耗
                    sleep(100)
                } catch (e: Exception) {
                    Log.e(TAG, "任务监控线程异常", e)
                }
            }

            // 监控结束清理
            isMonitorRunning = false
            Log.d(TAG, "任务监控线程退出")
        }
    }

    /**
     * 批量处理待取消的任务
     */
    private fun processPendingCancelTasks() {
        lock.lock()
        try {
            if (pendingCancelTaskIds.isNotEmpty()) {
                pendingCancelTaskIds.forEach { taskId ->
                    cancelTaskInternal(taskId)
                }
                pendingCancelTaskIds.clear()
            }
        } finally {
            lock.unlock()
        }
    }

    /**
     * 更新运行中任务的状态和进度
     */
    private fun updateRunningTasksStatus() {
        var i = 0
        while (i < runningTasksQueue.size) {
            val taskInfo = runningTasksQueue[i]
            val taskId = taskInfo.long_arr[0]
            try {
                // 获取当前任务状态
                val currentState = getTaskStateSafely(taskInfo, taskId)
                taskStateCache[taskId] = currentState

                if (currentState == 0) {
                    // 运行中：更新进度+通知
                    updateTaskProgressAndNotification(taskInfo, taskId)
                    i++
                } else {
                    // 任务完成/取消/失败：处理收尾逻辑
                    handleTaskCompletion(taskInfo, taskId, currentState)
                    // 移除后索引不递增（队列长度变化）
                }
            } catch (e: Exception) {
                Log.e(TAG, "更新任务状态失败 | 任务ID：$taskId", e)
                i++
            }
        }
    }

    /**
     * 安全获取任务状态（区分JNI/FFmpeg任务）
     */
    private fun getTaskStateSafely(taskInfo: TaskInfo, taskId: Long): Int {
        return if (taskInfo.int_arr[0] < 2) {
            // JNI任务：调用JNI接口
            getTaskState(tasksFactoryHandle, taskId)
        } else {
            // FFmpeg任务：调用AIDL接口（判空防护）
            ffmpegService?.getTaskState(taskId) ?: -1
        }
    }

    /**
     * 更新任务进度和通知
     */
    private fun updateTaskProgressAndNotification(taskInfo: TaskInfo, taskId: Long) {
        // 获取进度（区分JNI/FFmpeg任务）
        val progress = if (taskInfo.int_arr[0] < 2) {
            getProgress(tasksFactoryHandle, taskId)
        } else {
            ffmpegService?.getProgress(taskId) ?: 0f
        }
        taskProgressCache[taskId] = when {
            (progress < 0) &&  taskInfo.int_arr[0] !=2 -> 0f
            (progress > 1 ) &&  taskInfo.int_arr[0] !=2 -> 0.99f
            else -> progress
        }
        // 更新通知（有权限时）
        if (
//            checkNotificationsPermission(this) &&
            taskNotificationCache.containsKey(taskId)) {
            val progressPercent =
                if(
                    taskInfo.int_arr[0] !=2
                )
                {
                    ((taskProgressCache[taskId]?:0f) * 100).toInt()
                }
            else{
                     0
                }
            val notificationBuilder = taskNotificationCache[taskId]!!
            if(taskInfo.int_arr[0] !=2){
                notificationBuilder
                    .setProgress(100, progressPercent, false)
                    .setContentText("$progressPercent%")
            }
            else{
                notificationBuilder
                    .setContentText(TextsUtils.millisecondsToString(progress.toLong()))
            }

            notificationManager.notify(taskId.toInt(), notificationBuilder.build())
        }
    }

    /**
     * 处理任务完成/取消/失败的收尾逻辑（核心优化点）
     */
    private fun handleTaskCompletion(taskInfo: TaskInfo, taskId: Long, state: Int) {
        lock.lock()
        try {
            // 1. 原有逻辑：通知更新、资源释放、队列移除（完全不变）
            updateCompletionNotification(taskId, state)
            releaseTaskResource(taskInfo, taskId)
            cleanTaskCache(taskId)
            runningTasksQueue.remove(taskInfo)

            // ============== 新增：调用 Repository 保存任务到数据库 ==============
            // 获取全局 Repository
            val repository = AppApplication.INSTANCE.taskRepository
            // 协程执行（因为 saveTaskComplete 是 suspend 挂起函数）
            CoroutineScope(Dispatchers.IO).launch {
                repository.saveTaskComplete(taskInfo, taskId, state)
            }

        } catch (e: Exception) {
            Log.e(TAG, "处理任务收尾失败 | taskId:$taskId", e)
        } finally {
            lock.unlock()
        }
    }

    /**
     * 更新任务完成后的通知
     */
    private fun updateCompletionNotification(taskId: Long, state: Int) {
        if (
//            checkNotificationsPermission(this) &&
            taskNotificationCache.containsKey(taskId)) {
            val notificationBuilder = taskNotificationCache[taskId]!!
            notificationBuilder.setProgress(100, 0, false)
            val contentText = when (state) {
                1 -> getString(R.string.end_of_execution)
                2 -> getString(R.string.cancel_execution)
                -1 -> getString(R.string.execution_failed)
                else -> "unknown"
            }
            notificationBuilder.setContentText(contentText)
            notificationManager.notify(taskId.toInt(), notificationBuilder.build())
        }
    }

    /**
     * 释放任务资源（JNI/FFmpeg）
     */
    private fun releaseTaskResource(taskInfo: TaskInfo, taskId: Long) {
        try {
            if (taskInfo.int_arr[0] < 2) {
                releaseTask(tasksFactoryHandle, taskId)
            } else {
                ffmpegService?.releaseTask(taskId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "释放任务资源失败 | 任务ID：$taskId", e)
        }
    }

    /**
     * 保存任务到数据库（核心：异常捕获）
     */
//    private fun saveTaskToDatabase(taskInfo: TaskInfo, taskId: Long, state: Int) {
//        try {
//            val date = Date(System.currentTimeMillis())
//            val formatter = SimpleDateFormat(
//                "yyyy-MM-dd HH:mm:ss",
//                resources.configuration.locales[0]
//            )
//            val task = Task(
//                task_id = taskId,
//                type = taskInfo.int_arr[0],
//                status = state,
//                path = taskInfo.str_arr[0],
//                log_path = taskInfo.str_arr[1],
//                date = formatter.format(date)
//            )
//            // 数据库操作建议在子线程执行（优化：避免主线程阻塞）
//            thread(name = "TaskDBInsert") {
//                try {
//                    tasksDao.insertTask(task)
//                    Log.d(TAG, "任务持久化成功 | 任务ID：$taskId")
//                } catch (e: Exception) {
//                    Log.e(TAG, "任务持久化失败 | 任务ID：$taskId", e)
//                }
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "构建任务数据库实体失败 | 任务ID：$taskId", e)
//        }
//    }

    /**
     * 清理任务缓存（避免内存泄漏）
     */
    private fun cleanTaskCache(taskId: Long) {
        // 主动取消系统通知，防止残留
        try {
            notificationManager.cancel(taskId.toInt())
        } catch (e: Exception) {
            Log.e(TAG, "取消通知失败 taskId:$taskId")
        }
        taskProgressCache.remove(taskId)
        taskNotificationCache.remove(taskId)
    }

    /**
     * 调度等待队列的任务（填充空闲槽位）
     */
    private fun dispatchWaitingTasks() {
        val freeSlotNum = maxConcurrentTasks - runningTasksQueue.size
        var i = 0
        while (i < freeSlotNum && waitingTasksQueue.isNotEmpty()) {
            val taskInfo = waitingTasksQueue.first()
            try {
//                sleep(3000)
                startTaskSafely(taskInfo)
                waitingTasksQueue.removeFirst()
                runningTasksQueue.add(taskInfo)
                i++
            } catch (e: Exception) {
                Log.e(TAG, "启动任务失败 | 任务ID：${taskInfo.long_arr[0]}", e)
                i++
            }
        }
    }

    /**
     * 安全启动任务（区分JNI/FFmpeg）
     */
    private fun startTaskSafely(taskInfo: TaskInfo) {
        val taskId = taskInfo.long_arr[0]
        val taskType = taskInfo.int_arr[0]

        // 启动任务并记录状态
        val state = if (taskType < 2) {
            // JNI任务：调用JNI接口
            createAndStartTask(
                tasksFactoryHandle,
                taskInfo.int_arr.toIntArray(),
                taskInfo.long_arr.toLongArray(),
                taskInfo.float_arr.toFloatArray(),
                taskInfo.str_arr.toTypedArray()
            )
        } else {
            // FFmpeg任务：绑定服务+调用AIDL
            startFFmpegTask(taskInfo)
        }

        taskStateCache[taskId] = state
        taskProgressCache[taskId] = 0f

        // 创建任务通知（有权限时）
        createTaskNotification(taskInfo, taskId)
    }

    /**
     * 启动FFmpeg任务（绑定服务+判空防护）
     */
    private fun startFFmpegTask(taskInfo: TaskInfo): Int {
        // 原有临时绑定逻辑全部删除，改为：检查服务是否已绑定
        if (!isFFmpegServiceBound || ffmpegService == null) {
            Log.w(TAG, "FFmpegService 未就绪，等待绑定完成...")
            // 再次尝试绑定（兜底）
            bindFFmpegServiceInAdvance()

            // 等待服务连接（带超时）
            val isConnected = serviceConnectedLatch?.await(SERVICE_BIND_TIMEOUT, TimeUnit.MILLISECONDS) ?: false
            if (!isConnected || ffmpegService == null) {
                Log.e(TAG, "FFmpegService 最终绑定失败，启动任务失败")
                return -1
            }
        }

        // 服务就绪，执行任务
        return try {
            ffmpegService!!.createAndStartTask(
                taskInfo.int_arr.toIntArray(),
                taskInfo.long_arr.toLongArray(),
                taskInfo.str_arr.toTypedArray(),
                taskInfo.float_arr.toFloatArray()
            )
        } catch (e: Exception) {
            Log.e(TAG, "调用FFmpegService创建任务异常", e)
            -1
        }
    }

    /**
     * 创建任务通知
     */

    /**
     * 取消任务内部实现
     */
    private fun cancelTaskInternal(taskId: Long) {
        // 1. 从等待队列移除
        val waitingIterator = waitingTasksQueue.iterator()
        while (waitingIterator.hasNext()) {
            val taskInfo = waitingIterator.next()
            if (taskInfo.long_arr[0] == taskId) {
                waitingIterator.remove()
//                taskStateCache[taskId] = 2
                cleanTaskCache(taskId)
                CoroutineScope(Dispatchers.IO).launch {
                    AppApplication.INSTANCE.taskRepository.saveTaskComplete(taskInfo, taskId, 2)
                }
                Log.d(TAG, "从等待队列取消任务 | 任务ID：$taskId")
                return
            }
        }

        // 2. 从运行队列取消（区分JNI/FFmpeg）
        val runningIterator = runningTasksQueue.iterator()
        while (runningIterator.hasNext()) {
            val taskInfo = runningIterator.next()
            if (taskInfo.long_arr[0] == taskId) {
                runningIterator.remove()
                if (taskInfo.int_arr[0] >= 2) {
                    // FFmpeg任务：调用AIDL取消
                    ffmpegService?.cancelTask(taskId)
                } else {
                    // JNI任务：调用JNI取消
                    cancelTask(tasksFactoryHandle, taskId)
                }
                while(getTaskStateSafely(taskInfo, taskId)==0){
                    sleep(100)
                }
                releaseTaskResource(taskInfo,taskId)
//                taskStateCache[taskId] = 2
                cleanTaskCache(taskId)
                CoroutineScope(Dispatchers.IO).launch {
                    AppApplication.INSTANCE.taskRepository.saveTaskComplete(taskInfo, taskId, 2)
                }
                Log.d(TAG, "从运行队列取消任务 | 任务ID：$taskId")
                return
            }
        }

        // 3. 兜底：直接调用JNI取消（任务未在队列中）
//        if (taskStateCache[taskId] == 0 || !taskStateCache.containsKey(taskId)) {
//            cancelTask(tasksFactoryHandle, taskId)
//            taskStateCache[taskId] = 2
//            Log.d(TAG, "直接取消任务 | 任务ID：$taskId")
//        }
    }

    /**
     * 清理所有任务（销毁时）
     */
    private fun cleanAllTasks() {
        // 运行中任务
        runningTasksQueue.forEach { taskInfo ->
            val taskId = taskInfo.long_arr.getOrNull(0) ?: return@forEach
            try {
                // 区分JNI/FFmpeg任务释放资源
                if (taskInfo.int_arr.getOrNull(0) ?: 0 < 2) {
                    cancelTask(tasksFactoryHandle, taskId)
                    releaseTask(tasksFactoryHandle, taskId)
                } else {
                    ffmpegService?.cancelTask(taskId)
                    ffmpegService?.releaseTask(taskId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "清理运行任务失败 taskId:$taskId")
            }
        }
        runningTasksQueue.clear()

        // 等待中任务
        waitingTasksQueue.forEach { taskInfo ->
            val taskId = taskInfo.long_arr.getOrNull(0) ?: return@forEach
            try {
                if (taskInfo.int_arr.getOrNull(0) ?: 0 < 2) {
                    releaseTask(tasksFactoryHandle, taskId)
                } else {
                    ffmpegService?.releaseTask(taskId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "清理等待任务失败 taskId:$taskId")
            }
        }
        waitingTasksQueue.clear()

        // 清空缓存
        taskStateCache.clear()
        taskProgressCache.clear()
        pendingCancelTaskIds.clear()
    }

    /**
     * 解绑FFmpeg服务（判空防护）
     */
    private fun unbindFFmpegService() {
        if (isFFmpegServiceBound) {
            try {
                unbindService(ffmpegServiceConnection)
                isFFmpegServiceBound = false
                ffmpegService = null
                Log.d(TAG, "FFmpegService 解绑成功")
            } catch (e: Exception) {
                Log.e(TAG, "FFmpegService 解绑失败", e)
            }
        }
    }

    // ==================== 对外提供的辅助方法 ====================
    fun getAVStrInfo(path: String): String = getAudioAndVideoStrInfo(path)
    fun getTasksQueue(): List<TaskInfo> = allTasksQueue
    fun getWaitingTasksQueue(): List<TaskInfo> = waitingTasksQueue
    fun getRunningTasksQueue(): List<TaskInfo> = runningTasksQueue
    fun getFFmpegInfo(infoType: Int): String = getFFmpegStrInfo(infoType)

    // ==================== JNI接口 ====================
    private external fun initTasksFactory(): Long
    private external fun createAndStartTask(
        tasksFactory: Long,
        intArr: IntArray,
        longArr: LongArray,
        floatArr: FloatArray,
        strArr: Array<String>
    ): Int
    private external fun getTaskState(tasksFactory: Long, taskID: Long): Int
    private external fun releaseTask(tasksFactory: Long, taskID: Long)
    private external fun cancelTask(tasksFactory: Long, taskID: Long)
    private external fun getProgress(tasksFactory: Long, taskID: Long): Float
    private external fun getAudioAndVideoStrInfo(path: String): String
    private external fun getFFmpegStrInfo(infoType: Int): String


    // ==================== 静态代码块 ====================
    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }
}