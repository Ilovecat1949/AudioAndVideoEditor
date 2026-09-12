package com.example.audioandvideoeditor.transcoder

import android.media.MediaFormat
import com.example.audioandvideoeditor.entity.TaskInfo
import com.example.audioandvideoeditor.model.AudioEncodeConfig
import com.example.audioandvideoeditor.model.TaskState
import com.example.audioandvideoeditor.model.TranscodeTaskConfig
import com.example.audioandvideoeditor.model.VideoEncodeConfig
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

/**
 * 开箱即用且具备完整防呆机制的硬件转码 Task
 */
class HardwareTranscodeTask(

) {

    private var taskInfo: TaskInfo? = null
    private var transcoder: HardwareTranscoder? = null

    // 内部独立协程作用域
    private val taskScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    @Volatile
    private var runningJob: Job? = null

    // 使用 AtomicInteger 进行线程安全的状态管理
    private val currentState = AtomicInteger(TaskState.IDLE.code)
    @Volatile
    private var currentProgress: Float = 0f
    private fun setInfo(info: TaskInfo) {
        // 防呆 1：运行中或已终止的任务禁止重置 TaskInfo
        val state = currentState.get()
        if (state == TaskState.RUNNING.code || isTerminalState(state)) {
            return
        }
        this.taskInfo = info
    }
    /**
     * 在 HardwareTranscodeTask 内部解析 JSON 配置字符串
     */
    private fun parseTaskConfig(info: TaskInfo): TranscodeTaskConfig {
        val jsonStr = info.str_arr.getOrNull(2) ?: ""

        var videoConfig: VideoEncodeConfig? = null
        var audioConfig: AudioEncodeConfig? = null

        if (jsonStr.isNotBlank()) {
            runCatching {
                val json = JSONObject(jsonStr)

                // 1. 解析 Video 配置（防缺漏：optInt/optString 自动兜底）
                if (json.has("video")) {
                    val vObj = json.getJSONObject("video")
                    val width = vObj.optInt("width", 0)
                    val height = vObj.optInt("height", 0)

                    if (width > 0 && height > 0) {
                        videoConfig = VideoEncodeConfig(
                            targetWidth = width,
                            targetHeight = height,
                            bitrate = vObj.optInt("bitrate", 4_000_000),      // 缺失默认 4Mbps
                            frameRate = vObj.optInt("frameRate", 30),         // 缺失默认 30fps
                            iFrameInterval = vObj.optInt("iFrameInterval", 1),
                            mimeType = vObj.optString("mime", MediaFormat.MIMETYPE_VIDEO_AVC)
                        )
                    }
                }
                // 2. 解析 Audio 配置
                if (json.has("audio")) {
                    val aObj = json.getJSONObject("audio")
                    audioConfig = AudioEncodeConfig(
                        sampleRate = aObj.optInt("sampleRate", 44100),
                        channelCount = aObj.optInt("channelCount", 2),
                        bitrate = aObj.optInt("bitrate", 128_000),
                        mimeType = aObj.optString("mime", MediaFormat.MIMETYPE_AUDIO_AAC),
                        isPassthrough = aObj.optBoolean("isPassthrough", false)
                    )
                }
            }.onFailure {
                // JSON 格式解析失败时的日志兜底，防止崩溃
                android.util.Log.e("HardwareTranscodeTask", "Failed to parse config JSON", it)
            }
        }

        // 防漏兜底：如果 JSON 没传音频配置，默认开启 128k AAC 编码保底
        if (audioConfig == null) {
            audioConfig = AudioEncodeConfig(
                sampleRate = 44100,
                channelCount = 2,
                bitrate = 128_000,
                mimeType = MediaFormat.MIMETYPE_AUDIO_AAC,
                isPassthrough = true
            )
        }

        return TranscodeTaskConfig(videoConfig = videoConfig, audioConfig = audioConfig)
    }
    private fun taskInit(): Int {
        // 防呆 2：使用 CAS 确保只有 IDLE 或 FAILED 状态才可以初始化
        if (!currentState.compareAndSet(TaskState.IDLE.code, TaskState.INITED.code) &&
            !currentState.compareAndSet(TaskState.FAILED.code, TaskState.INITED.code)
        ) {
            // 当前已处于 INITED、RUNNING 或 CANCELED 等状态，忽略重复初始化操作
            return if (currentState.get() == TaskState.INITED.code) 0 else -1
        }
        // 防呆 3：入参完整性强校验
        val info = taskInfo
        if (info == null || info.str_arr.size<3) {
            currentState.set(TaskState.FAILED.code)
            return -1
        }
        val taskConfig=parseTaskConfig(info)
        // 重置进度与转码器
        currentProgress=0f
        transcoder = HardwareTranscoder(taskConfig, object : ITranscodeListener {
            override fun onStart() {
                currentState.set(TaskState.RUNNING.code)
            }

            override fun onProgress(progress: Float) {
                currentProgress=progress
            }

            override fun onSuccess(outputPath: String) {
                currentProgress=100f
                currentState.set(TaskState.SUCCESS.code)
            }

            override fun onError(e: Exception) {
                currentState.set(TaskState.FAILED.code)
            }

            override fun onCanceled() {
                currentState.set(TaskState.CANCELED.code)
            }
        })

        return 0
    }

    private fun start() {
        // 防呆 4：严格的 CAS 状态防重（仅允许 INITED -> RUNNING）
        // 如果当前处于 RUNNING、SUCCESS、CANCELED 等非 INITED 状态，直接拦截退出
        if (!currentState.compareAndSet(TaskState.INITED.code, TaskState.RUNNING.code)) {
            return
        }

        val info = taskInfo
        if (info == null) {
            currentState.set(TaskState.FAILED.code)
            return
        }

        // 防呆 5：并发 Job 防重（如果上一次 Job 还在运行，不重复创建）
        if (runningJob?.isActive == true) {
            return
        }

        runningJob = taskScope.launch {
            try {
                transcoder?.startTranscode(info.str_arr[1], info.str_arr[0])
            } catch (e: CancellationException) {
                // 响应协程取消
                currentState.set(TaskState.CANCELED.code)
            } catch (e: Exception) {
                currentState.set(TaskState.FAILED.code)
            }
        }
    }

    fun startTask(info: TaskInfo){
        setInfo(info)
        taskInit()
        start()
    }

    fun cancel() {
        val state = currentState.get()
        // 防呆 6：已处于终态（成功/失败/已取消）时忽略 cancel()
        if (isTerminalState(state)) {
            return
        }

        currentState.set(TaskState.CANCELED.code)

        // 标记引擎取消并中断协程
        transcoder?.cancel()
        runningJob?.cancel()
    }

    fun release() {
        cancel()
        taskScope.cancel() // 销毁整个线程作用域
        transcoder = null
        currentState.set(TaskState.IDLE.code)
    }

    fun getProgress(): Float {
        return currentProgress
    }

    fun getState(): Int {
        val state = currentState.get()
        return if(state== TaskState.IDLE.code || state== TaskState.INITED.code ||state==TaskState.RUNNING.code){
            TaskState.UNFINISHED.code
        } else{
            state
        }
    }

    /**
     * 判断是否属于不可逆的终止状态
     */
    private fun isTerminalState(state: Int): Boolean {
        return state == TaskState.SUCCESS.code ||
                state == TaskState.FAILED.code ||
                state == TaskState.CANCELED.code
    }
}