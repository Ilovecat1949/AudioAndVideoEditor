package com.example.audioandvideoeditor.services

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.PixelFormat
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
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.toColorInt
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.application.AppApplication
import com.example.audioandvideoeditor.model.RecordingConfig
import com.example.audioandvideoeditor.model.RecordingStatus
import com.example.audioandvideoeditor.navigation.Destination
import com.example.audioandvideoeditor.recorder.engine.IRecorderEngine
import com.example.audioandvideoeditor.recorder.engine.RecorderEngineFactory
import com.example.audioandvideoeditor.utils.ConfigsUtils.loadRecordConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.File
import java.io.IOException

class RecordingService : Service() {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    // 视频编码器和混合器
    private var videoEncoder: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var videoTrackIndex = -1
    private val TAG="RecordingService"
    private val notificationId = 1
    private val channelId = "screen_record_channel"
    private var channelName ="record"

    private val VIDEO_MIME_TYPE = "video/avc"
//    private val VIDEO_WIDTH = 720
//    private val VIDEO_HEIGHT = 1280
//    private val VIDEO_BIT_RATE = 5 * 1024 * 1024 // 5 Mbps
//    private val VIDEO_FRAME_RATE = 30

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
    private var fileName=""
    // 在 RecordingService 类内部成员变量区添加：
    private var recordingConfig: RecordingConfig = RecordingConfig()

    // 在 RecordingService.kt 中添加状态枚举定义

    // 1. 引入核心状态机变量，默认为空闲状态
    var currentStatus = RecordingStatus.IDLE
        private set // 限制只能在 Service 内部修改状态，保证状态安全


    // 1. 创建一个绑定自定义 Job 和 IO 线程的协程作用域
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var m_pfd: ParcelFileDescriptor?=null

    // 🌟 悬浮窗核心管理三大件
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    // 🌟 手势防卡死算法的核心记账本
    private var lastX = 0f
    private var lastY = 0f
    private var isMoving = false

    // 🌟 核心函数 1：将小球 View 钉上系统桌面
    private fun showFloatingWindow() {
        // 1. 绝对防御：如果已经挂载过了，或者系统压根没给悬浮窗权限，静默闭嘴，绝不莽撞报错
        if (floatingView != null || !android.provider.Settings.canDrawOverlays(this)) {
            return
        }

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            // 2. 动态加载我们刚刚在 方案A 中雕刻完美的 XML 布局
            floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_ball, null)

            // 3. 焊死最严苛、最具商业兼容性的窗口 LayoutParams 参数大闸
            layoutParams = WindowManager.LayoutParams().apply {
                // 现代 Android (API 26+) 强制要求的顶级应用叠加图层
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

                // 🌟 决定成败的 Flags：
                // FLAG_NOT_FOCUSABLE: 绝对不抢占手机焦点，确保用户能边录屏边操作底下的任意 App
                // FLAG_LAYOUT_IN_SCREEN: 允许小球坐标相对于全屏计算
                flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
//                        or WindowManager.LayoutParams.FLAG_SECURE // 👈 就是这一行！强制系统在录屏流中屏蔽此窗口
                        )

                // 边缘全透明裁剪支持
                format = PixelFormat.TRANSLUCENT

                // 宽高包裹内容，初始落位在屏幕右侧居中
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.TOP or Gravity.START
                x = resources.displayMetrics.widthPixels - 200 // 初始靠右
                y = resources.displayMetrics.heightPixels / 2  // 居中
            }

            // 4. 注入黄金手势与状态联动（核心算法在下方定义）
            initFloatingWindowTouchAndClicks()

            // 5. 正式向系统申请，把小球推向全屏最顶层
            windowManager?.addView(floatingView, layoutParams)

            // 6. 🌟 挂载成功后，根据 Service 当下的真实状态，初次同步刷新一下小球的图标
            updateFloatingWindowUI()

        } catch (e: Exception) {
            Log.e("RecordingService", "挂载桌面悬浮球失败，已安全降级: ${e.message}")
            floatingView = null
        }
    }

    // 🌟 核心函数 2：根据 Service 内部最高真理状态，刷新小球自己的图标状态
    private fun updateFloatingWindowUI(status: RecordingStatus = currentStatus) {
        val view = floatingView ?: return
        val btnControl = view.findViewById<ImageView>(R.id.btnControl) ?: return
        // 【新增】获取停止按钮引用
        val btnStop = view.findViewById<ImageView>(R.id.btnStop) ?: return
        btnStop.imageTintList = ColorStateList.valueOf("#FF4D4F".toColorInt()) // 警示红
        when (status) {
            RecordingStatus.RECORDING -> {
                btnControl.isEnabled = true
                btnControl.setImageResource(R.drawable.pause_circle_24px) // 录制中显示“暂停”图标
                btnControl.imageTintList = ColorStateList.valueOf("#FF4D4F".toColorInt()) // 警示红

                // 【新增】录制中显示停止按钮
                btnStop.visibility = View.VISIBLE
                btnStop.isEnabled = true
            }
            RecordingStatus.PAUSED -> {
                btnControl.isEnabled = true
                btnControl.setImageResource(R.drawable.play_circle_24px) // 暂停中显示“继续”图标
                btnControl.imageTintList = ColorStateList.valueOf("#FFA940".toColorInt()) // 暖阳橙

                // 【新增】暂停中保持显示停止按钮
                btnStop.visibility = View.VISIBLE
                btnStop.isEnabled = true
            }
            RecordingStatus.SAVING -> {
                btnControl.isEnabled = false // 保存中禁用点击，防止重复触方向
                btnControl.imageTintList = ColorStateList.valueOf("#8C8C8C".toColorInt()) // 锁定灰

                // 【新增】保存中禁用停止按钮
                btnStop.isEnabled = false
            }
            RecordingStatus.IDLE, RecordingStatus.STOPPED -> {
                btnControl.isEnabled = true
                btnControl.setImageResource(R.drawable.play_circle_24px) // 待机显示“开始”图标
                btnControl.imageTintList = ColorStateList.valueOf("#FFFFFF".toColorInt()) // 高级白

                // 【新增】待机状态隐藏停止按钮
                btnStop.visibility = View.GONE
            }
            else -> {

            }
        }
    }

    // 🌟 核心函数 3：解绑卸载（大管家死了，必须干干净净把桌面的东西收走）
    private fun removeFloatingWindow() {
        floatingView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e("RecordingService", "撤销悬浮球时发生微小抖动: ${e.message}")
            }
        }
        floatingView = null
        windowManager = null
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun initFloatingWindowTouchAndClicks() {
        val view = floatingView ?: return
        val cardContainer = view.findViewById<View>(R.id.cardContainer) ?: return
        val btnControl = view.findViewById<View>(R.id.btnControl) ?: return
        // 🌟 【新增】获取布局中独立的 btnStop 停止按钮
        val btnStop = view.findViewById<View>(R.id.btnStop) ?: return
        val btnClose = view.findViewById<View>(R.id.btnClose) ?: return

        // 🌟 用于平滑吸附的属性动画器（声明在监听外层，方便随时取消）
        var edgeAnimator: android.animation.ValueAnimator? = null

        cardContainer.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 1. 如果上一次松手后的吸附动画还没播完，用户又按住了，立刻强行掐断动画，跟着手指走
                    edgeAnimator?.cancel()

                    // 2. 记录手指按下时的绝对屏幕坐标
                    lastX = event.rawX
                    lastY = event.rawY
                    isMoving = false
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - lastX
                    val deltaY = event.rawY - lastY

                    // 3. 🌟 黄金防抖阈值判定：位移绝对值大于 10 像素时才真正激活拖拽
                    if (!isMoving && (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10)) {
                        isMoving = true
                    }

                    if (isMoving) {
                        // 4. 实时跟着手指坐标更新 Window 位置
                        params.x += deltaX.toInt()
                        params.y += deltaY.toInt()

                        // 5. 💡 安全边界防御：防止用户把小球完全拖出屏幕顶部状态栏或底部以外
                        val metrics = resources.displayMetrics
                        if (params.y < 0) params.y = 0
                        val maxY = metrics.heightPixels - 150 // 预留底部虚拟键高度
                        if (params.y > maxY) params.y = maxY

                        // 6. 刷新顶级图层渲染位置
                        windowManager?.updateViewLayout(view, params)

                        // 7. 重新对齐时间轴轴心
                        lastX = event.rawX
                        lastY = event.rawY
                    }
                }

                MotionEvent.ACTION_UP -> {
                    // 8. 🌟 核心平滑吸附算法
                    if (isMoving) {
                        val metrics = resources.displayMetrics
                        val screenWidth = metrics.widthPixels
                        val viewWidth = view.width

                        // 计算松手时小球的起点 X 坐标
                        val startX = params.x

                        // 判定：小球中心点过了屏幕中线就贴右，否则贴左
                        // 🌟 细节优化：左右两侧特意留出 16 像素的安全外边距（防全面屏手势冲突）
                        val targetX = if (startX + viewWidth / 2 < screenWidth / 2) {
                            16 // 靠左安全距离
                        } else {
                            screenWidth - viewWidth - 16 // 靠右安全距离
                        }

                        // 9. 启动纯原生属性动画，将 X 坐标从 startX 平滑推演到 targetX
                        edgeAnimator = android.animation.ValueAnimator.ofInt(startX, targetX).apply {
                            duration = 250 // 250毫秒，最符合人类视觉的丝滑档位

                            // 🌟 物理减速插值器：模拟松手后缓缓刹车停靠的物理直觉
                            interpolator = android.view.animation.DecelerateInterpolator()

                            addUpdateListener { animator ->
                                val currentX = animator.animatedValue as Int
                                layoutParams?.x = currentX
                                // 🌟 高度控制的核心：在更新过程中，params.y 绝对不去碰它，完美锁定原有高度！
                                if (floatingView != null) {
                                    windowManager?.updateViewLayout(view, layoutParams)
                                }
                            }
                        }
                        edgeAnimator?.start()
                    }
                }
            }
            // 返回 true 意味着手势位移被本容器完美吞掉并安全消费，绝不向外发生事件泄漏
            return@setOnTouchListener true
        }

        // 🌟 【改动】子View点击事件：控制按钮由“开始/停止”重构为“开始/暂停/继续”
        btnControl.setOnClickListener {
            val intent = Intent(this, RecordingService::class.java)
            when (currentStatus) {
                RecordingStatus.RECORDING -> {
                    intent.action = "ACTION_PAUSE_RECORDING" // 录制中 -> 暂停
                }
                RecordingStatus.PAUSED -> {
                    intent.action = "ACTION_RESUME_RECORDING" // 暂停中 -> 继续
                }
                RecordingStatus.IDLE, RecordingStatus.STOPPED -> {
                    intent.action = "ACTION_START_RECORDING" // 待机中 -> 开始
                }
                else -> return@setOnClickListener
            }
            startService(intent)
        }

        // 🌟 【新增】停止按钮：专门负责 停止录制并保存视频
        btnStop.setOnClickListener {
            if (currentStatus == RecordingStatus.RECORDING || currentStatus == RecordingStatus.PAUSED) {
                val intent = Intent(this, RecordingService::class.java).apply {
                    action = "ACTION_STOP_RECORDING"
                }
                startService(intent)
            }
        }

        btnClose.setOnClickListener {
            // 按照你刚才的高明调整：只移除小球 UI，完全不惊动后台服务与录制状态
            removeFloatingWindow()
        }
    }

    // RecordingService.kt 核心调度变动部分：
    private var recorderEngine: IRecorderEngine? = null


    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
        channelName = this.getString(R.string.record)

// 🌟 优化点 1：将通知渠道的创建完全收拢到 onCreate() 地基阶段，仅在创建服务时初始化一次
        createNotificationChannel()
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
                "ACTION_START_RECORDING" -> {
                   // 防御：若已经在录制中，直接拦截
                    if (currentStatus == RecordingStatus.RECORDING || currentStatus == RecordingStatus.PAUSED) return START_NOT_STICKY
                    // 🌟 增加：向唯一信任源报备：启动中，正在拉起系统投影权限弹窗
                    AppApplication.INSTANCE.taskRepository.updateRecordingState(RecordingStatus.PENDING)
                    // 🌟 核心改变：从 Intent 中安全提取配置对象并保存
                    recordingConfig = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra("EXTRA_RECORDING_CONFIG", RecordingConfig::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra("EXTRA_RECORDING_CONFIG")
                    }) ?: loadRecordConfig(this)

                    // 🌟 顺手把音频类型从配置类里剥离出来（完美承接你现有的音频逻辑）
//                    recordAudioType = recordingConfig.audioType

                    filePath = intent.getStringExtra("EXTRA_FILE_PATH") ?: ""
                    fileName= intent.getStringExtra("EXTRA_MEDIA_NAME") ?: ""
                    mediaUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra("EXTRA_MEDIA_URI", Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra("EXTRA_MEDIA_URI")
                    }

                    val resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED)
                    val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra("data", Intent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra("data")
                    }

                    if (resultCode != Activity.RESULT_OK || data == null) {
                        Toast.makeText(this, this.getString(R.string.toast_record_permission_denied), Toast.LENGTH_SHORT).show()
//                        stopSelf()
                        return START_NOT_STICKY
                    }

                    val notification = createRecordingNotification(this,intent.action)
                    // 🌟 针对 Android 14+ 进行前台服务类型安全适配
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(
                            notificationId,
                            notification,
                            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                        )
                    } else {
                        startForeground(notificationId, notification)
                    }

                    val recordingCallback = object : MediaProjection.Callback() {
                        override fun onStop() {
                            super.onStop()
                            mediaProjection?.unregisterCallback(this)
                        }
                    }
                    mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
                    mediaProjection?.registerCallback(recordingCallback, null)

                    if(mediaUri!=null){
                            m_pfd=contentResolver.openFileDescriptor(mediaUri!!, "w")
                    }
                    else{
                        val file = File(filePath)
                        // 读写模式（若文件不存在则自动创建）
                        m_pfd= ParcelFileDescriptor.open(
                            file,
                            ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE
                        )
                    }

                    // 🌟 2. 核心架构改变：不再等待 UI 的 Binder 连通！前台通道拉起后，直接同步触发实质录制
                    //   startRecording()
                    // 1. 通过工厂创建具体引擎策略
                    recorderEngine = RecorderEngineFactory.createEngine(this, recordingConfig)
                    // 2. 统一驱动引擎启动
                    recorderEngine?.start(recordingConfig, mediaProjection!!, m_pfd!!)
                    Log.d("RecordingService", "Recording started!")
                    // 🌟 状态平滑流转：成功启动后，将状态切为 RECORDING
                    currentStatus = RecordingStatus.RECORDING
                    // 🌟 增加：硬件真正就绪，向全局广播：正在录制中
                    AppApplication.INSTANCE.taskRepository.updateRecordingState(RecordingStatus.RECORDING)
                    // 🌟 增量：只要用户满足配置需要，就尽力而为把小球挂上去
                    showFloatingWindow()
                    // 🌟 增量：让小球图标瞬间同步变为 [⏹️ 停止]
                    updateFloatingWindowUI()
                }
                "ACTION_STOP_RECORDING" -> {
                       // 只要是在“录屏中”或者“暂停中”，都可以收尾保存
                    if (currentStatus == RecordingStatus.RECORDING || currentStatus == RecordingStatus.PAUSED) {
                        recorderEngine?.stop()
                        recorderEngine = null
                        stopRecording() // 停止编码，释放本轮 VirtualDisplay，执行 IO 写入

                        // 刷新前台通知，明确告诉用户大管家还活着，处于就绪待命状态
                        val manager = getSystemService(NotificationManager::class.java)
                        manager?.notify(notificationId,
                            createRecordingNotification(this,intent.action)
                        )
                        // 状态安全回归待机，主页面按钮全自动变回绿色
                        AppApplication.INSTANCE.taskRepository.updateRecordingState(RecordingStatus.IDLE)
                        updateFloatingWindowUI()
                        // TODO: 未来在这里触发：悬浮球 UI 切换回静态的 [▶️开始] 待机样式
                    }
                }
                // 🌟 3. 预留可扩展桩：暂停录制
                "ACTION_PAUSE_RECORDING" -> {
                    if (currentStatus==RecordingStatus.RECORDING) {
                        Log.i(TAG, "收到暂停指令，正在挂载编码器...")

                        // 🛠️ 后期实现核心：
                        // mediaRecorder.pause() 或者 AeroFFmpegSDK.markPauseTime()
//                        pauseRecording()
                        // 同步更新全局状态契约
                        recorderEngine?.pause()
                        AppApplication.INSTANCE.taskRepository.updateRecordingState(RecordingStatus.PAUSED)

                        // 刷新通知栏文本为“录屏已暂停”
                        val manager = getSystemService(NotificationManager::class.java)
                        manager?.notify(notificationId,
                            createRecordingNotification(this,intent.action)
                            )
                        currentStatus= RecordingStatus.PAUSED
                        updateFloatingWindowUI()
                        // TODO: 未来在这里触发：悬浮球样式切换为 [▶️继续] + [⏹️停止]
                    }
                }

                // 🌟 4. 预留可扩展桩：继续录制
                "ACTION_RESUME_RECORDING" -> {
                    if (currentStatus == RecordingStatus.PAUSED) {
                        Log.i(TAG, "收到继续录制指令，恢复数据流...")

                        // 🛠️ 后期实现核心：
                        // mediaRecorder.resume() 或者 AeroFFmpegSDK.adjustTimeDelta()
//                        resumeRecording()
                        recorderEngine?.resume()
                        AppApplication.INSTANCE.taskRepository.updateRecordingState(RecordingStatus.RECORDING)

                        val manager = getSystemService(NotificationManager::class.java)
                        manager?.notify(notificationId, createRecordingNotification(this,intent.action))
                        currentStatus=RecordingStatus.RECORDING
                        updateFloatingWindowUI()
                        // TODO: 未来在这里触发：悬浮球样式切换回 [⏸️暂停] + [⏹️停止]
                    }
                }

                // 🌟 5. 终结大管家
                "ACTION_DESTROY_SERVICE" -> {
                    // 🌟 增量：彻底毁灭前，把桌面的小球揭掉
                    removeFloatingWindow()
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    /**
     * 🌟 核心新增：极其严密的逆序解耦资源释放链，保障线上环境 0 闪退
     */
    private fun releaseMediaResources() {
        // 1. 先停 Recorder 捕获
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e("RecordingService", "释放：mediaRecorder stop 失败", e)
        } finally {
            mediaRecorder?.release()
            mediaRecorder = null
        }

        // 2. 释放虚拟显示区
        try {
            virtualDisplay?.release()
        } catch (e: Exception) {
            Log.e("RecordingService", "释放：virtualDisplay release 失败", e)
        } finally {
            virtualDisplay = null
        }

        // 3. 最后断开系统核心投影令牌（防止底层重入或空指针 Crash）
        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            Log.e("RecordingService", "释放：mediaProjection stop 失败", e)
        } finally {
            mediaProjection = null
        }
        // 🌟 在末尾增加 PFD 的安全关闭
        try {
            m_pfd?.close()
        } catch (e: Exception) {
            Log.e(TAG, "释放：ParcelFileDescriptor close 失败", e)
        } finally {
            m_pfd = null
        }
    }

    fun startRecording() {
        if (currentStatus != RecordingStatus.IDLE) {
            return
        } // 防御：只有空闲时才能触发录制
        try {
            // 创建通知，启动前台服务
//            createNotificationChannel()
//            val notification = createNotification()
//            startForeground(notificationId, notification)

            // 配置 MediaRecorder
            val metrics = resources.displayMetrics
            // 🌟 防御性设计：如果配置类传了 0，代表用当前手机全屏宽高；
            // 如果传了具体数值（比如 1080），我们就用它。同时通过 `/ 2 * 2` 确保它们一定是偶数。
            val screenWidth = if (recordingConfig.videoWidth > 0) {
                (recordingConfig.videoWidth / 2) * 2
            } else {
                (metrics.widthPixels / 2) * 2
            }

            val screenHeight = if (recordingConfig.videoHeight > 0) {
                (recordingConfig.videoHeight / 2) * 2
            } else {
                (metrics.heightPixels / 2) * 2
            }
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
                    m_pfd = contentResolver.openFileDescriptor(mediaUri!!, "w")
                    if (m_pfd != null) {
                        setOutputFile(m_pfd!!.fileDescriptor)
                    }
                }
                else{
                    setOutputFile(filePath)
                }
                // 文件路径
//                val videoFile = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "screen_record_${System.currentTimeMillis()}.mp4")
//                setOutputFile(videoFile.absolutePath)
                // 视频配置
                setVideoSize(screenWidth, screenHeight) // 示例分辨率
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoEncodingBitRate(recordingConfig.videoBitrate) // 5 Mbps
                setVideoFrameRate(recordingConfig.frameRate)
                prepare()
            }

            // 创建虚拟显示器
            val displayMetrics = resources.displayMetrics
            val densityDpi = displayMetrics.densityDpi
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "RecordingDisplay",
                screenWidth, screenHeight, // 与 MediaRecorder 视频尺寸一致
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null,
                null
            )

            mediaRecorder?.start()
            Log.d("RecordingService", "Recording started!")
            // 🌟 状态平滑流转：成功启动后，将状态切为 RECORDING
            currentStatus = RecordingStatus.RECORDING
            // 🌟 增加：硬件真正就绪，向全局广播：正在录制中
            AppApplication.INSTANCE.taskRepository.updateRecordingState(RecordingStatus.RECORDING)
        } catch (e: IOException) {
            Log.e("RecordingService", "startRecording failed", e)
            stopRecording()
            currentStatus = RecordingStatus.IDLE // 发生异常退回 IDLE
            AppApplication.INSTANCE.taskRepository.updateRecordingState(RecordingStatus.IDLE)
        }
    }

    private fun stopRecording() {
        // 防御：只有在录制中或暂停时，才可以执行停止和清理
        if (currentStatus != RecordingStatus.RECORDING && currentStatus != RecordingStatus.PAUSED) {
            return
        }

// 🌟 1. 替换原本的零散释放，引入严密的逆序安全销毁
//        releaseMediaResources()

        currentStatus = RecordingStatus.IDLE

        // 🌟 2. 契约落位：正常录制结束，立即更新全局状态
        AppApplication.INSTANCE.taskRepository.updateRecordingState(RecordingStatus.IDLE)

// 3. 🌟 外科手术式修改：彻底抛弃旧的 saveInfo() 协程，直接甩锅给 Repository
        // 把当前录好的 Uri 和 路径 扔过去，Service 就算彻底交差了！
        AppApplication.INSTANCE.taskRepository.saveRecordingResult(filePath, mediaUri,fileName)
        mediaUri?.let { uri ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0) // 解锁，全系统可见
                }
                // 原地刷新媒体库
                contentResolver.update(uri, contentValues, null, null)
                Log.d("RecordingService", "视频文件落盘成功，IS_PENDING 已成功解除锁！")
            }
        }
        filePath=""
        mediaUri=null
        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            Log.e("RecordingService", "释放：mediaProjection stop 失败", e)
        } finally {
            mediaProjection = null
        }

        // 🌟 在末尾增加 PFD 的安全关闭
        try {
            m_pfd?.close()
        } catch (e: Exception) {
            Log.e(TAG, "释放：ParcelFileDescriptor close 失败", e)
        } finally {
            m_pfd = null
        }
        // 4. 清爽地自毁，没有任何丢数据的包袱
//        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
//        stopSelf()
    }

    private fun pauseRecording() {
        if (currentStatus == RecordingStatus.RECORDING && mediaRecorder != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mediaRecorder?.pause()
                    currentStatus = RecordingStatus.PAUSED
                    AppApplication.INSTANCE.taskRepository.updateRecordingState(RecordingStatus.PAUSED)
                    updateFloatingWindowUI(RecordingStatus.PAUSED)
                }
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }

    private fun resumeRecording() {
        if (currentStatus == RecordingStatus.PAUSED && mediaRecorder != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mediaRecorder?.resume()
                    currentStatus = RecordingStatus.RECORDING
                    AppApplication.INSTANCE.taskRepository.updateRecordingState(RecordingStatus.RECORDING)
                    updateFloatingWindowUI(RecordingStatus.RECORDING)
                }
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }

    // 🌟 优化点 3：规范前台通知构建，移除了内部重复手写的 NotificationChannel 创建硬编码，保持纯粹性
// 🌟 优化点 3：规范前台通知构建，引入完整的 暂停 / 继续 / 停止 控制 Action
    private fun createRecordingNotification(context: Context, action_value: String?): Notification {
        val intent = Intent(this, Class.forName("com.example.audioandvideoeditor.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("TARGET_ROUTE", Destination.Recording.route)
            putExtra("TASK_ID", -1L)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, -1, intent, flags)

        // 1. 【新增】定义三个独立的 PendingIntent 指令
        val stopIntent = Intent(context, RecordingService::class.java).apply { action = "ACTION_STOP_RECORDING" }
        val stopPendingIntent = PendingIntent.getService(context, 0, stopIntent, flags)

        val pauseIntent = Intent(context, RecordingService::class.java).apply { action = "ACTION_PAUSE_RECORDING" }
        val pausePendingIntent = PendingIntent.getService(context, 1, pauseIntent, flags)

        val resumeIntent = Intent(context, RecordingService::class.java).apply { action = "ACTION_RESUME_RECORDING" }
        val resumePendingIntent = PendingIntent.getService(context, 2, resumeIntent, flags)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.movie_edit_24px)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // 2. 【改动】依据动作与当前状态动态配置通知内容及 Action 按钮
        when (action_value) {
            "ACTION_START_RECORDING", "ACTION_RESUME_RECORDING" -> {
                builder
                    .setContentTitle(this.getString(R.string.app_name))
                    .setContentText(this.getString(R.string.recording))
                    .addAction(
                        NotificationCompat.Action(
                            IconCompat.createWithResource(context, R.drawable.pause_circle_24px),
                            context.getString(R.string.pause),
                            pausePendingIntent
                        )
                    )
                    .addAction(
                        NotificationCompat.Action(
                            IconCompat.createWithResource(context, R.drawable.stop_circle_24px),
                            context.getString(R.string.stop),
                            stopPendingIntent
                        )
                    )
            }
            "ACTION_PAUSE_RECORDING" -> {
                builder
                    .setContentTitle(this.getString(R.string.app_name))
                    .setContentText(this.getString(R.string.notification_text_paused))
                    .addAction(
                        NotificationCompat.Action(
                            IconCompat.createWithResource(context, R.drawable.play_circle_24px),
                            context.getString(R.string.resume),
                            resumePendingIntent
                        )
                    )
                    .addAction(
                        NotificationCompat.Action(
                            IconCompat.createWithResource(context, R.drawable.stop_circle_24px),
                            context.getString(R.string.stop),
                            stopPendingIntent
                        )
                    )
            }
            "ACTION_STOP_RECORDING" -> {
                builder
                    .setContentTitle(this.getString(R.string.app_name))
                    .setContentText(this.getString(R.string.notification_text_stopped))
            }
        }
        return builder.build()
    }

    // 🌟 优化点 4：将基础地基抽象为标准单点职责函数，重要度统一对齐为 HIGH 确保前台不被系统轻易强杀
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = this@RecordingService.getString(R.string.notification_channel_desc)

            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }


    // 创建通知渠道

    override fun onDestroy() {
// 4. 清爽解耦：onDestroy 不再去调用 stopRecording()，彻底斩断循环调用链路
        super.onDestroy()

// 🌟 核心修改：用安全释放链进行最终资源兜底
//        releaseMediaResources()
        recorderEngine?.release()
        // 🌟 契约落位：确保无论何种原因服务销毁，全局状态必定安全复位回归 IDLE，不锁死前台 UI
        AppApplication.INSTANCE.taskRepository.updateRecordingState(RecordingStatus.IDLE)

        // 🌟 核心修改：前台通知安全移除销毁
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)

        serviceScope.cancel()
// 🌟 增量：确保无论服务因何种原因毁灭（比如被系统强杀），桌面的小球必须跟着一起消失，防止残留
        removeFloatingWindow()
    }


    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }
}


// 🌟 优化后极其纯净的空壳 Binder，仅作为服务连接的存根，不再向外部暴露任何业务状态
class RecordingBinder(private val service: RecordingService) : Binder() {
    fun getContext(): Context = service
}