package com.example.audioandvideoeditor.services

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.AudioPlaybackCaptureConfiguration
import android.media.MediaCodec
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.dao.AppDatabase
import com.example.audioandvideoeditor.dao.TasksDao
import com.example.audioandvideoeditor.entity.Task
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class RecordingService : Service() {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    // 视频编码器和混合器
    private var videoEncoder: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var videoTrackIndex = -1

    private val notificationId = 1
    private val channelId = "screen_record_channel"
    private var channelName ="record"

    private val VIDEO_MIME_TYPE = "video/avc"
    private val VIDEO_WIDTH = 720
    private val VIDEO_HEIGHT = 1280
    private val VIDEO_BIT_RATE = 5 * 1024 * 1024 // 5 Mbps
    private val VIDEO_FRAME_RATE = 30
    private var tasksDao: TasksDao?=null
    var isRecording = false
        private set
    private val mBinder = RecordingBinder(this)
    private var recordAudioType=0
    fun setRecordAudioType(type :Int){
        recordAudioType=type
    }
    private var filePath=""
    fun setFilePath(path :String){
        filePath=path
    }
    private var mediaUri: Uri?=null
    fun setMediaUri(uri :Uri?){
        mediaUri=uri
    }
    private var pfd: ParcelFileDescriptor?=null
    override fun onCreate() {
        super.onCreate()
        tasksDao= AppDatabase.getDatabase(this).taskDao()
        mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
        channelName = this.getString(R.string.record)
    }
    fun setupInternalAudioCapture(
        mediaProjection: MediaProjection,
        data: Intent // 从 MediaProjectionManager 返回的 Intent
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Android 10 以下不适用
            return
        }

        // 1. 定义音频格式
//        val audioFormat = AudioFormat.Builder()
//            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
//            .setSampleRate(44100)
//            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
//            .build()

        // 2. 创建 AudioPlaybackCaptureConfiguration
        val captureConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)      // 匹配媒体播放
            .addMatchingUsage(AudioAttributes.USAGE_GAME)       // 匹配游戏声音
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)    // 匹配其他声音
            .build()

        // 3. 创建 AudioRecord 实例
        // 注意：使用 AudioRecord 需要单独处理音频编码和混流，它不能直接连到 MediaRecorder。
        // 在实际的录屏应用中，你需要：
        // a) 使用 AudioRecord 获取 PCM 数据。
        // b) 使用 MediaCodec 编码 PCM 数据为 AAC。
        // c) 使用 MediaMuxer 将 AAC 编码后的音频和 H.264 编码后的视频混流。

        // **简化的 MediaRecorder 替代方案 (Android Q+):**
        // 尽管 MediaRecorder 不能直接用 AudioPlaybackCapture，但在 Android 10+ 的录屏场景中，
        // Google 建议直接使用 MediaRecorder.AudioSource.MIC，并在 MediaProjection 启动时，
        // 如果用户授权了，系统会自动处理内部音频的捕获和混流。
        // 然而，为了更精确的控制，使用 AudioRecord + MediaCodec + MediaMuxer 是更可靠的方法。

        // **最简且能工作的实践 (依赖系统自动混流):**
        // 假设系统会处理 MediaProjection 授权后的内部音频混流（这通常适用于录屏）
//        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC) // 仍然使用 MIC 源
//        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

        // 如果用户的 MediaProjection 授权对话框中包含“录制音频”选项并被勾选，
        // 系统在 MediaRecorder 处于录制状态时，可能会自动将内部音频源的流数据混入。
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                "ACTION_START" -> {
                    if (isRecording) return START_NOT_STICKY
                    val resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED)
                    // 使用新的、类型安全的 getParcelableExtra 方法
                    val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra("data", Intent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra("data")
                    }

                    if (resultCode != Activity.RESULT_OK || data == null) {
                        Toast.makeText(this, "录屏权限被拒绝，无法启动服务", Toast.LENGTH_SHORT).show()
                        stopSelf()
                        return START_NOT_STICKY
                    }
                    // 确保通知渠道在创建通知前已存在
                    createNotificationChannel()
                    val notification = createRecordingNotification(this)//createNotification()
                    startForeground(notificationId, notification)
                    // 2. 注册匿名回调对象 (更简洁!)
                    val recordingCallback = object : MediaProjection.Callback() {
                        override fun onStop() {
                            super.onStop()
                            // 在用户/系统撤销权限时，执行清理逻辑
//                            stopRecording()
                            // 注意：我们必须手动注销它，尽管 stop() 会释放资源，
                            // 但最好在 onStop() 被调用后，立即执行清理。
                            mediaProjection?.unregisterCallback(this)
                        }
                    }
                    mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
                    // 注册回调，使用当前线程的 Handler (null)
                    mediaProjection?.registerCallback(recordingCallback, null)
//                    startRecording()
//                    Handler().postDelayed({
//                        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
//                         startRecording()
//                    }, 1000)
                }
                "ACTION_STOP" -> {
                    stopRecording()
                }

            }
        }
        return START_NOT_STICKY
    }

    fun startRecording() {
        try {
            // 创建通知，启动前台服务
//            createNotificationChannel()
//            val notification = createNotification()
//            startForeground(notificationId, notification)

            // 配置 MediaRecorder
            mediaRecorder =
                if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.S) {
                    MediaRecorder(this)
                }
                else{
                    MediaRecorder()
                }
                .apply {
                if(recordAudioType==1){
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                }

                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

                if(mediaUri!=null){
                    pfd = contentResolver.openFileDescriptor(mediaUri!!, "w")
                    if (pfd != null) {
                        setOutputFile(pfd!!.fileDescriptor)
                    }
                }
                else{
                    setOutputFile(filePath)
                }
                // 文件路径
//                val videoFile = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "screen_record_${System.currentTimeMillis()}.mp4")
//                setOutputFile(videoFile.absolutePath)

                // 视频配置
                setVideoSize(VIDEO_WIDTH, VIDEO_HEIGHT) // 示例分辨率
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoEncodingBitRate(VIDEO_BIT_RATE) // 5 Mbps
                setVideoFrameRate(VIDEO_FRAME_RATE)
                prepare()
            }

            // 创建虚拟显示器
            val displayMetrics = resources.displayMetrics
            val densityDpi = displayMetrics.densityDpi
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "RecordingDisplay",
                VIDEO_WIDTH, VIDEO_HEIGHT, // 与 MediaRecorder 视频尺寸一致
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null,
                null
            )

            mediaRecorder?.start()
            Log.d("RecordingService", "Recording started!")
            isRecording=true
        } catch (e: IOException) {
            Log.e("RecordingService", "startRecording failed", e)
            stopRecording()
        }
    }

    private fun stopRecording() {
        isRecording=false
        mediaRecorder?.apply {
            stop()
            reset()
            release()
        }
        pfd?.close()
        pfd=null
        mediaRecorder = null

        virtualDisplay?.release()
        virtualDisplay = null

        mediaProjection?.stop()
        mediaProjection = null

        // 1. 解除前台状态
        // 使用 STOP_FOREGROUND_REMOVE 标志，表示移除前台状态的同时，也移除状态栏通知。
        // 这是最常见且完整的解除前台状态操作。
        ServiceCompat.stopForeground(
            /* service = */ this,
            /* flags = */ ServiceCompat.STOP_FOREGROUND_REMOVE
        )
        stopSelf()
        if(!saveInfoFlag){
            saveInfoFlag=true
            Thread {
                saveInfo()
            }.start()
        }
        Log.d("RecordingService", "Recording stopped.")
    }



    // 创建前台服务通知
    private fun createNotification(): Notification {
        val stopIntent = Intent(this, RecordingService::class.java).apply { action = "ACTION_STOP" }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            Notification.Builder(this)
        }
        val notificationAction = Notification.Action.Builder(
            android.R.drawable.ic_media_pause, this.getString(R.string.stop), stopPendingIntent
        ).build()
        return builder
            .setContentTitle(this.getString(R.string.recording))
            .setContentText(this.getString(R.string.stop))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(notificationAction)
            .build()
    }
    private fun createRecordingNotification(context: Context): Notification {
        // 1. 创建通知渠道 (Android O 及以上版本必须)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName, // 例如: "屏幕录制服务"
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelName // 例如: "录屏控制和状态"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // 2. 创建停止操作的 PendingIntent
        val stopIntent = Intent(context, RecordingService::class.java).apply {
            action = "ACTION_STOP"// 假设这是您的停止 Action
        }

        // Flag_IMMUTABLE 针对 Android 12+ 是推荐做法
        val stopPendingIntent = PendingIntent.getService(
            context,
            0, // Request code
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 3. 构建通知
        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(this.getString(R.string.recording)) // 正在录制...
            .setContentText(this.getString(R.string.stop)) // 点击停止

            // 推荐使用 Material Icons 或您自己的 Drawable
            .setSmallIcon(R.drawable.movie_edit_24px) // 替换为您应用自己的图标

            // 确保通知不会被滑动清除
            .setOngoing(true)

            // 4. 添加操作按钮 (使用现代 Builder)
            .addAction(
                // 使用 androidx.core.graphics.drawable.IconCompat (推荐)
                NotificationCompat.Action(
                    IconCompat.createWithResource(context, R.drawable.baseline_pause_24), // 假设您有停止图标
                    context.getString(R.string.stop), // 停止
                    stopPendingIntent
                )
            )
            // 设定通知的优先级
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        return builder.build()
    }


    // 创建通知渠道
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "用于屏幕录制服务的前台通知"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }
    private var saveInfoFlag=false
    private fun saveInfo(){
        var id=0L
        val max_id= tasksDao?.getMaxTaskId()
        if(max_id!=null){
            id=max_id+1
        }
        val date= Date(System.currentTimeMillis())
        val formatter= SimpleDateFormat("yyyy-MM-dd HH:mm:ss", resources.configuration.locales[0])
        if(mediaUri!=null) {
            tasksDao?.insertTask(
                Task(
                    task_id = id,
                    type = 4,
                    status = 1,
                    uri = mediaUri.toString(),
                    date = formatter.format(date)
                )
            )
        }
        else if(filePath.isNotEmpty()){
                tasksDao?.insertTask(
                Task(
                    task_id = id,
                    type = 4,
                    status = 1,
                    path = filePath,
                    date = formatter.format(date)
                )
            )
        }
        mediaUri=null
        filePath=""
    }
    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }
}


class RecordingBinder (service: RecordingService) : Binder(){
    private var service: RecordingService
    init{
        this.service=service
    }
    fun setRecordAudioType(type :Int){
        service.setRecordAudioType(type)
    }
    fun setFilePath(path :String){
        service.setFilePath(path)
    }
    fun setMediaUri(uri :Uri?){
        service.setMediaUri(uri)
    }
    fun startRecording(){
        service.startRecording()
    }
    fun getContext():Context{
        return service
    }
    fun isRecording():Boolean{
        return service.isRecording
    }
//    var stopRecordingCallback:()->Unit={}
}