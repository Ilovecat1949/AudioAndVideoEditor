package com.example.audioandvideoeditor.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audioandvideoeditor.dao.AppDatabase
import com.example.audioandvideoeditor.entity.Task
import com.example.audioandvideoeditor.services.RecordingBinder
import com.example.audioandvideoeditor.services.RecordingService
import com.example.audioandvideoeditor.utils.ConfigsUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import java.io.File

data class RecordingState(
    val isRecording: Boolean = false,
    val isPermissionsGranted: Boolean = false,
    val showPermissionsDialog: Boolean = false,
    val videoSettings: RecordingVideoSettings = RecordingVideoSettings(),
    val audioSettings: RecordingAudioSettings = RecordingAudioSettings(),
    val recordedVideos: List<String> = emptyList(),
    val currentError: String? = null
)

data class RecordingVideoSettings(
    val resolution: String = "1080p",
    val bitRate: String = "4 Mbps",
    val fps: String = "30"
)

data class RecordingAudioSettings(
    val bitRate: String = "128 kbps",
    val sampleRate: String = "44.1 kHz",
    val channels: String = "Mono"
)
class RecordingViewModel(application: Application) : AndroidViewModel(application) {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var   startMediaProjectionLauncher: ActivityResultLauncher<Intent>
    fun startRecordingService(context: Context, resultCode: Int, data: Intent) {
        val serviceIntent = Intent(context, RecordingService::class.java).apply {
            action = "ACTION_START"
            putExtra("resultCode", resultCode)
            putExtra("data", data)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
        context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE) // 绑定Service
        binder_flag=true
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
            action = "ACTION_STOP"
        }
        context.startService(stopIntent)
    }
    private fun requestMediaProjection() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startMediaProjectionLauncher.launch(captureIntent)
    }
    private lateinit  var recordingBinder: RecordingBinder
    var binder_flag=false
    private set
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            recordingBinder = service as RecordingBinder
            if(!isRecording) {
                recordingBinder.setRecordAudioType(ConfigsUtils.recordAudioType)
                // 1. 创建文件 URI
                val videoFileName = "ScreenRecord_${System.currentTimeMillis()}.mp4"
                val newVideoUri = createNewMovieUri(recordingBinder.getContext(), videoFileName)
                recordingBinder.setMediaUri(newVideoUri)
                recordingBinder.startRecording()
                isRecording = true
            }
        }
        override fun onServiceDisconnected(name: ComponentName) {

        }
    }
    private val _uiState = MutableStateFlow(RecordingState())
    val uiState: StateFlow<RecordingState> = _uiState.asStateFlow()

    fun onStartRecording() { /* TODO: Call start recording service */ }
    fun onStopRecording(context: Context ) {
        val serviceIntent = Intent(context, RecordingService::class.java).apply {
            action =  "ACTION_STOP"  // 关键：设置停止 Action
        }
        // 发送带有 STOP Action 的 Intent。服务会接收到，执行 stopSelf()
        context.startService(serviceIntent)
        isRecording=false
        if(binder_flag){
            context.unbindService(connection)
            binder_flag=false
        }
    }
    fun onUnbindService(context: Context){
        if(binder_flag){
            context.unbindService(connection) // 绑定Service
            binder_flag=false
        }
    }
    fun onBindService(context: Context){
        if(!binder_flag and isRecording){
            val serviceIntent = Intent(context, RecordingService::class.java)
            context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE) // 绑定Service
            binder_flag=true
        }
    }
    fun onRequestPermissions() { /* TODO: Request permissions */ }
    fun onSettingsChanged(settings: Any) { /* TODO: Update settings */ }
    fun onDismissError() { /* TODO: Clear current error */ }
    fun onPermissionsResult(isGranted: Boolean) { /* TODO: Handle permission result */ }
    fun onRefreshVideos() { /* TODO: Refresh video list from disk */ }
    var isRecording by mutableStateOf(false)
//    fun isRecording():Boolean{
//       return recordingBinder.isRecording()
//    }

    // 假设您在 Application 或 Repository 层获得了 Dao 实例
    private val tasksDao = AppDatabase.getDatabase(application).taskDao()

    // 将 Flow 转换为 StateFlow，供 Compose 观察
    val tasksListState: StateFlow<List<Task>> = tasksDao.loadTasksByType(4)
        .stateIn(
            scope = viewModelScope,
            // 当 UI 不再观察时，Flow 停止收集，节省资源
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList() // 初始值为空列表
        )

    val mutex = Mutex()
    val thumbnailsMaxNum=100
    val thumbnailBitmapArray=ArrayList<Pair<String, Bitmap>>()

    lateinit var videoPlay:(uri: String, route:String)->Unit
}