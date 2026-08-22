package com.example.audioandvideoeditor.ui.recording

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.LruCache
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audioandvideoeditor.application.AppApplication
import com.example.audioandvideoeditor.entity.Task
import com.example.audioandvideoeditor.model.RecordingConfig
import com.example.audioandvideoeditor.model.RecordingStatus
import com.example.audioandvideoeditor.services.RecordingBinder
import com.example.audioandvideoeditor.services.RecordingService
import com.example.audioandvideoeditor.utils.ConfigsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

data class RecordingState(
    val isSessionActive: Boolean = false,// 🌟 标记录制会话是否开启（录制中或暂停中）
    val isPaused: Boolean = false, // 🌟 新增：标记当前是否处于暂停状态
    val isRecording: Boolean = false,     // 仅当真正处于录制状态时为 true
    val isPermissionsGranted: Boolean = false,
    val showPermissionsDialog: Boolean = false,
//    val videoSettings: RecordingVideoSettings = RecordingVideoSettings(),
//    val audioSettings: RecordingAudioSettings = RecordingAudioSettings(),
    val recordedVideos: List<String> = emptyList(),
    val currentError: String? = null,
    // 🌟 精准补充：让 UI 状态树能够持有这个录屏配置，给一个完全默认的实例即可
    val selectedConfig: RecordingConfig = RecordingConfig(),
    // 🌟 增量追加：是否显示“开始录屏”时的即时推荐小球弹窗
    val showOverlayRecommendDialog: Boolean = false
)

//data class RecordingVideoSettings(
//    val resolution: String = "1080p",
//    val bitRate: String = "4 Mbps",
//    val fps: String = "30"
//)
//
//data class RecordingAudioSettings(
//    val bitRate: String = "128 kbps",
//    val sampleRate: String = "44.1 kHz",
//    val channels: String = "Mono"
//)

class RecordingViewModel() : ViewModel() {
    private val _uiState = MutableStateFlow(RecordingState())
    val uiState: StateFlow<RecordingState> = _uiState.asStateFlow()
    // 🌟 精准修改：不再给硬编码的死测试数字，诞生时直接拿磁盘里的真理赋予它
    var currentConfig by mutableStateOf(
        ConfigsUtils.loadRecordConfig(AppApplication.INSTANCE)
    )
    init {
// 🌟 对齐修改：将刚刚通过 loadRecordConfig 初始化好的 currentConfig 喂给前台 UI 状态树
        _uiState.update { it.copy(selectedConfig = currentConfig) }
        // 🌟 精准加入这段：无视 Service 存亡，实时同步 Repository 的全局录屏状态
        viewModelScope.launch {
            AppApplication.INSTANCE.taskRepository.recordingState.collect { globalState ->
                _uiState.update {
                    it.copy(
                        isSessionActive = globalState == RecordingStatus.RECORDING || globalState == RecordingStatus.PAUSED,
                        isRecording = globalState == RecordingStatus.RECORDING,
                        isPaused = globalState == RecordingStatus.PAUSED
                    )
                }
            }
        }
    }

    // 2. 新增：发送暂停录屏指令
    fun onPauseRecording(context: Context) {
        val serviceIntent = Intent(context, RecordingService::class.java).apply {
            action = "ACTION_PAUSE_RECORDING"
        }
        context.startService(serviceIntent)
    }

    // 3. 新增：发送继续录屏指令
    fun onResumeRecording(context: Context) {
        val serviceIntent = Intent(context, RecordingService::class.java).apply {
            action = "ACTION_RESUME_RECORDING"
        }
        context.startService(serviceIntent)
    }

    // 🌟 关闭即时推荐弹窗
    fun onDismissRecommendDialog() {
        _uiState.update { it.copy(showOverlayRecommendDialog = false) }
    }

    // 🌟 当用户在推荐弹窗中点击“开启并去授权”时调用
    fun onAcceptRecommendAndGo(context: Context) {
        _uiState.update { it.copy(showOverlayRecommendDialog = false) }

        // 1. 先把本地配置里的开关改成 true，让账本记住用户的意愿
//        val updatedConfig = currentConfig.copy(isFloatingWindowEnabled = true)
//        updateRecordingConfig(updatedConfig)

        // 2. 快递将用户送往系统授权页
        val intent = Intent(
            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context.packageName}".toUri()
        )
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // 🌟 安全封装：由 ViewModel 内部统一、单向地管理状态的开启
    fun showOverlayRecommendDialog(flag: Boolean) {
        _uiState.update { it.copy(showOverlayRecommendDialog = flag) }
    }
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var   startMediaProjectionLauncher: ActivityResultLauncher<Intent>
    // 在 RecordingViewModel 类内部（比如 isRecording 变量附近）添加：

    fun startRecordingService(context: Context, resultCode: Int, data: Intent) {
// 先在本地为主线程准备好全量参数
        val date= Date(System.currentTimeMillis())
        val formatter= SimpleDateFormat("yyyyMMddHHmmss", context.resources.configuration.locales[0])
        val videoFileName = "${formatter.format(date)}.mp4"
        val newVideoUri = createNewMovieUri(context, videoFileName)
        val serviceIntent = Intent(context, RecordingService::class.java).apply {
            action = "ACTION_START_RECORDING"
            putExtra("resultCode", resultCode)
            putExtra("data", data)
            // 🌟 将原本依赖 Binder 赋值的参数，改由 Intent 快递打包带走
            val metrics = context.resources.displayMetrics
            val isLandscape = metrics.widthPixels > metrics.heightPixels

            var targetWidth = if (currentConfig.videoWidth > 0) currentConfig.videoWidth else metrics.widthPixels
            var targetHeight = if (currentConfig.videoHeight > 0) currentConfig.videoHeight else metrics.heightPixels
            // 🌟 防御：如果当前屏幕是横屏，但设置的分辨率是竖屏，自动反转长短边避免画面拉伸
            if (isLandscape && targetWidth < targetHeight) {
                val temp = targetWidth
                targetWidth = targetHeight
                targetHeight = temp
            } else if (!isLandscape && targetWidth > targetHeight) {
                val temp = targetWidth
                targetWidth = targetHeight
                targetHeight = temp
            }
            // 确保偶数对齐
            val finalWidth = targetWidth and -2
            val finalHeight = targetHeight and -2
            // 🌟 核心改变：删掉原本零散的 EXTRA_AUDIO_TYPE，改成一发入魂的配置对象快递
            putExtra("EXTRA_RECORDING_CONFIG", currentConfig.copy(
                videoWidth = finalWidth,
                videoHeight = finalHeight
            ))
            putExtra("EXTRA_MEDIA_URI", newVideoUri)
            putExtra("EXTRA_MEDIA_NAME", videoFileName)
        }

        // 瞬间送达 Service：系统拉起通知的同时，Service 已经在主线程完成参数解析并执行 startRecording()
        ContextCompat.startForegroundService(context, serviceIntent)
    }
    private fun createNewMovieUri(context: Context, fileName: String, mimeType: String = "video/mp4"): Uri? {
        // MediaStore 是访问共享存储空间中媒体文件的推荐方式
        val contentResolver = context.contentResolver

        val contentValues = ContentValues().apply {
            // 设置文件名
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            // 设置 MIME 类型
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            // 设置视频文件保存在 Movies 目录下
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+) 使用 RELATIVE_PATH
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
                // 确保视频文件立即可见
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        // 插入新的条目，返回 Uri
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        return contentResolver.insert(collection, contentValues)
    }
    private fun stopRecordingService(context: Context) {
        val stopIntent = Intent(context, RecordingService::class.java).apply {
            action = "ACTION_STOP_RECORDING"
        }
        context.startService(stopIntent)
    }
    private fun requestMediaProjection() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startMediaProjectionLauncher.launch(captureIntent)
    }
    private lateinit  var recordingBinder: RecordingBinder

    fun onStartRecording() { /* TODO: Call start recording service */ }
    fun onStopRecording(context: Context ) {
        val serviceIntent = Intent(context, RecordingService::class.java).apply {
            action =  "ACTION_STOP_RECORDING"  // 关键：设置停止 Action
        }
        // 发送带有 STOP Action 的 Intent。服务会接收到，执行 stopSelf()
        context.startService(serviceIntent)
    }
    fun onRequestPermissions() { /* TODO: Request permissions */ }
    // 🌟 严格补充：当用户在前台界面点选了新的分辨率、帧率或码率时调用此函数
    fun updateRecordingConfig(newConfig: RecordingConfig) {
        // 1. 同步刷新 ViewModel 内存中的配置变量，确保后续启动录屏时拿走的是最新的
        currentConfig = newConfig

        // 2. 驱动前台 UI 状态流刷新，前台 Compose 观察到 selectedConfig 变化会全自动重组更新界面
        _uiState.update { it.copy(selectedConfig = newConfig) }

        // 3. 🌟 顺手推一把：调用你写在 ConfigsUtils 里的落盘方法，通知磁盘异步记住这次修改
        ConfigsUtils.saveRecordConfig(AppApplication.INSTANCE, newConfig)
    }
    fun onDismissError() { /* TODO: Clear current error */ }
    fun onPermissionsResult(isGranted: Boolean) { /* TODO: Handle permission result */ }
    fun onRefreshVideos() { /* TODO: Refresh video list from disk */ }



    // 将 Flow 转换为 StateFlow，供 Compose 观察
    val tasksListState: StateFlow<List<Task>> = AppApplication.INSTANCE.taskRepository.loadTasksByType(4)
        .stateIn(
            scope = viewModelScope,
            // 当 UI 不再观察时，Flow 停止收集，节省资源
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList() // 初始值为空列表
        )

    val mutex = Mutex()
    val thumbnailsMaxNum=100
    // LruCache 内部已实现多线程同步，无需手动管理 List 顺序与移除逻辑
    val thumbnailCache = LruCache<String, Bitmap>(thumbnailsMaxNum)

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
                AppApplication.INSTANCE.taskRepository.deleteTaskById(taskToDelete!!.task_id)
                taskToDelete = null
            }
        }
    }
}