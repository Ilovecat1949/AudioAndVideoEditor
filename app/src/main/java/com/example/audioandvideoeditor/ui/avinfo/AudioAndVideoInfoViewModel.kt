package com.example.audioandvideoeditor.ui.avinfo

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.application.AppApplication
import com.example.audioandvideoeditor.entity.MediaInfo
import com.example.audioandvideoeditor.entity.TaskInfo
import com.example.audioandvideoeditor.utils.ConfigsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioAndVideoInfoViewModel: ViewModel()  {
    private var info_text = ""
    private var editedMap= HashMap<String, String>()
    // 原始媒体信息（只读）[cite: 4]
    var info by mutableStateOf(MediaInfo())
        private set

    // 编辑草稿（直接复用 MediaInfo）[cite: 4]
    var draftInfo by mutableStateOf(MediaInfo())
        private set

    // 页面状态
    var isEditMode by mutableStateOf(false)
        private set

    var isProcessing by mutableStateOf(false)
        private set

    // 重度编辑：另存为弹窗控制状态
    var showSaveAsDialog by mutableStateOf(false)
        private set

    var showCreationTimePickerDialog by mutableStateOf(false)

    var showResolutionPickerDialog by mutableStateOf(false)

    // 属性选择弹窗控制状态
    var showVideoCodecPickerModal by mutableStateOf(false)
    var showVideoProfilePickerModal by mutableStateOf(false)
    var showPixelFormatPickerModal by mutableStateOf(false)
    var showAudioCodecPickerModal by mutableStateOf(false)

    // 2. 弹窗控制状态
    var showAudioProfilePickerModal by mutableStateOf(false)

    // 2. 弹窗控制状态
    var showSampleRatePickerModal by mutableStateOf(false)

    // 属于重度编辑（必须通过 FFmpeg 命令行另存为任务执行）的 Key 集合
    private val heavyEditKeys = setOf(
        "resolution",
        "fps",
        "video_bit_rate",
        "sample_rate",
        "audio_bit_rate",
        "video_codec",
        "video_profile",
        "pixel_format",
        "audio_codec",
        "audio_profile" // 👈 新增
    )

    // 1. 采样率预设选项 (Hz)
    val sampleRateOptions = listOf(
        "8000",   // 电话/低带宽
        "16000",  // 语音识别 / VoIP
        "32000",  // 广播 quality
        "44100",  // 标准 CD 音质 (最常用)
        "48000",  // 影视 / 专业音频标准 (最常用)
        "96000"   // 高解析度音频 (Hi-Res)
    )

    // 视频编码：UI 显示文本 -> FFmpeg 命令行编码器标识
    val videoCodecMap = mapOf(
        "H.264(AVC)" to "libx264",
        "H.265(HEVC)" to "libx265",
        "MPEG-4" to "mpeg4",
//        "VP8" to "vp8",
//        "VP9" to "vp9"
    )

    // 音频编码：UI 显示文本 -> FFmpeg 命令行编码器标识
    val audioCodecMap = mapOf(
        "AAC" to "aac",
        "MP3" to "libmp3lame",
        "OPUS" to "opus",
        "FLAC" to "flac"
    )

    val audioProfileOptions = listOf(
        "aac_low",   // AAC-LC (最通用)
        "aac_he",    // HE-AAC v1
        "aac_he_v2", // HE-AAC v2
        "aac_ld",    // Low Delay
        "aac_eld"    // Enhanced Low Delay
    )
    fun getInfo(path: String) {
        viewModelScope.launch {
            setInfoText(path)
        }
    }

    private suspend fun setInfoText(path: String) = withContext(Dispatchers.Default) {
        val text = AppApplication.INSTANCE.taskRepository.getAVInfo(path)

        val newInfo = MediaInfo()
        newInfo.initInfo(text)

        withContext(Dispatchers.Main) {
            info_text = text
            info = newInfo
            print(text)
        }
    }

    // 开启编辑模式：复制当前数据作为草稿[cite: 4]
    fun startEditing() {
        editedMap.clear() // 🌟 开启编辑时清空上一次残留的 Diff
        draftInfo = info.copy()
        isEditMode = true
    }

    // 取消编辑[cite: 4]
    fun cancelEditing() {
        editedMap.clear() // 🌟 取消时也清空
        isEditMode = false
        showSaveAsDialog = false
    }
    // 是否包含重度编辑指令
    fun hasHeavyEdit(): Boolean {
        return editedMap.keys.any { it in heavyEditKeys }
    }

    // --- 轻度元数据编辑更新 ---
    fun updateDraftTitle(newTitle: String) {
        draftInfo = draftInfo.copy(title = newTitle)
        updateDiff("title", info.title, newTitle)
    }

    fun updateDraftArtist(newArtist: String) {
        draftInfo = draftInfo.copy(artist = newArtist)
        updateDiff("artist", info.artist, newArtist)
    }

    fun updateDraftDescription(newDesc: String) {
        draftInfo = draftInfo.copy(description = newDesc)
        updateDiff("comment", info.description, newDesc)
    }

    fun updateDraftCopyright(newCopyright: String){
        draftInfo=draftInfo.copy(copyright = newCopyright)
        updateDiff("copyright",info.copyright,newCopyright)
    }

    fun updateDraftAlbum(newAlbum: String){
        draftInfo=draftInfo.copy(album = newAlbum)
        updateDiff("album",info.album,newAlbum)
    }

    fun updateDraftGenre(newGenre: String){
        draftInfo=draftInfo.copy(genre = newGenre)
        updateDiff("genre",info.genre,newGenre)
    }

    fun updateDraftCreationTime(newTime: String){
        draftInfo=draftInfo.copy(creation_time = newTime)
        updateDiff("creation_time",info.creation_time,newTime)
    }

    fun rotateDraft() {
        val nextRotation = (draftInfo.rotation + 90) % 360
        draftInfo = draftInfo.copy(rotation = nextRotation)
        updateDiff("rotation", info.rotation.toString(), nextRotation.toString())
    }

    // --- 新增重度编辑更新函数 (编码格式, Profile, 像素格式) ---
    fun updateDraftVideoCodec(newCodec: String) {
        draftInfo = draftInfo.copy(video_codec_type = newCodec)
        updateDiff("video_codec", info.video_codec_type, newCodec)
    }

    fun updateDraftVideoProfile(newProfile: String) {
        draftInfo = draftInfo.copy(video_profile = newProfile)
        updateDiff("video_profile", info.video_profile, newProfile)
    }

    fun updateDraftPixelFormat(newPixFmt: String) {
        draftInfo = draftInfo.copy(pixel_format = newPixFmt)
        updateDiff("pixel_format", info.pixel_format, newPixFmt)
    }

    fun updateDraftAudioCodec(newCodec: String) {
        draftInfo = draftInfo.copy(audio_codec_type = newCodec)
        updateDiff("audio_codec", info.audio_codec_type, newCodec)
    }

    fun updateDraftAudioProfile(newProfile: String) {
        draftInfo = draftInfo.copy(audio_profile = newProfile)
        updateDiff("audio_profile", info.audio_profile, newProfile)
    }

    // 接收选择器传回的采样率字符串
    fun updateDraftSampleRateSelected(sampleRateStr: String) {
        val rate = sampleRateStr.toIntOrNull() ?: info.sample_rate
        draftInfo = draftInfo.copy(sample_rate = rate)
        updateDiff("sample_rate", info.sample_rate.toString(), rate.toString())
    }

    // --- 重度编辑更新 (分辨率, 帧率, 码率, 采样率) ---
    fun updateDraftResolution(widthStr: String, heightStr: String) {
        val w = widthStr.toIntOrNull() ?: draftInfo.width
        val h = heightStr.toIntOrNull() ?: draftInfo.height
        draftInfo = draftInfo.copy(width = w, height = h)
        val newRes = "${w}x${h}"
        val oldRes = "${info.width}x${info.height}"
        updateDiff("resolution", oldRes, newRes)
    }

    fun updateDraftFps(fpsStr: String) {
        val fpsVal =fpsStr.toFloatOrNull()
        if(fpsStr.isEmpty() || fpsVal==null || fpsVal<=0){
            draftInfo = draftInfo.copy(frame_rate = -1f)
            updateDiff("fps", String.format(Locale.US, "%.2f", info.frame_rate), "-1.00")
            return
        }
        draftInfo = draftInfo.copy(frame_rate = fpsVal)
        updateDiff("fps", String.format(Locale.US, "%.2f", info.frame_rate), String.format(Locale.US, "%.2f", fpsVal))
    }

    fun updateDraftVideoBitrate(kbpsStr: String) {
        val kbps = kbpsStr.toLongOrNull()
        if (kbpsStr.isEmpty() || kbps ==null  || kbps<=0 ) {
            // 用户清空输入框，标记为未设置或重置
            draftInfo = draftInfo.copy(video_bit_rate = -1L)
            updateDiff("video_bit_rate", (info.video_bit_rate / 1000).toString(), "-1")
            return
        }
        draftInfo = draftInfo.copy(video_bit_rate = kbps * 1000)
        updateDiff("video_bit_rate", (info.video_bit_rate / 1000).toString(), kbps.toString())
    }

    fun updateDraftSampleRate(sampleRateStr: String) {
        val rate = sampleRateStr.toIntOrNull()
        if(sampleRateStr.isEmpty() || rate==null || rate<=0){
            draftInfo = draftInfo.copy(sample_rate = -1)
            updateDiff("sample_rate", info.sample_rate.toString(), "-1")
            return
        }
        draftInfo = draftInfo.copy(sample_rate = rate)
        updateDiff("sample_rate", info.sample_rate.toString(), rate.toString())
    }

    private fun updateDiff(key: String, oldValue: String, newValue: String) {
        if (oldValue != newValue) {
            editedMap[key] = newValue
        } else {
            editedMap.remove(key)
        }
    }

    // 点击保存的主入口
    fun onSaveClicked(path: String, onFastEditComplete: (Boolean) -> Unit) {
        if (hasHeavyEdit()) {
            // 包含重度编辑，触发另存为弹窗
            showSaveAsDialog = true
        } else {
            // 纯轻量编辑，执行原子覆盖
            applyFastEdit(path, onFastEditComplete)
        }
    }

    fun dismissSaveAsDialog() {
        showSaveAsDialog = false
    }

    // 提交轻度编辑修改
    fun applyFastEdit(path: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            isProcessing = true
            val success =
                if(editedMap.isEmpty()){
                   true
                }
            else{
                    withContext(Dispatchers.IO) {
                        // 调用在 TaskRepository 中写好的 nativeApplyFastEdit 接口
                        var editCommands=""
                        for ((key, value) in editedMap) {
                            editCommands+="${key}:${value}\n"
                        }
                        applyEditToOriginalFile(path,editCommands)
                    }
                }
            isProcessing = false
            if (success) {
                isEditMode = false
                // 重新加载刷新最新的媒体信息[cite: 4]
                getInfo(path)
            }
            onComplete(success)
        }
    }

    suspend fun applyEditToOriginalFile(
        originalPath: String,
        editCommands: String
    ): Boolean = withContext(Dispatchers.IO) {
        val srcFile = File(originalPath)
        if (!srcFile.exists()) return@withContext false

        // 1. 构造一个临时的输出路径（推荐在同级目录下加后缀，确保在同一个文件系统中，移动性能最高）
        val tempFile = File(srcFile.parent, "${srcFile.name}.tmp_${System.currentTimeMillis()}")

        try {
            // 2. 调用 JNI 执行轻量修改（将数据写入临时文件）
            val result = AppApplication.INSTANCE.taskRepository.applyFastEdit(
                input_path = srcFile.absolutePath,
                output_path = tempFile.absolutePath,
                edit_commands= editCommands
            )

            // 3. 判断 JNI 执行结果
            if (result == 0 && tempFile.exists() && tempFile.length() > 0) {
                // 执行成功：用临时文件【原子替换/覆盖】原文件
                // 在 Java/Kotlin 中，最标准的替换方式是先删除原文件（或使用 StandardCopyOption.REPLACE_EXISTING）
                val isSuccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        Files.move(
                            tempFile.toPath(),
                            srcFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE
                        )
                        true
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    // 低版本 Android 兜底方案
                    if (srcFile.delete()) {
                        tempFile.renameTo(srcFile)
                    } else false
                }

                return@withContext isSuccess
            } else {
                // JNI 执行失败：删除残留的临时文件，保护原文件不受任何影响
                if (tempFile.exists()) tempFile.delete()
                return@withContext false
            }
        } catch (e: Exception) {
            // 发生异常，清理临时文件
            if (tempFile.exists()) tempFile.delete()
            return@withContext false
        }
    }

    /**
     * 🌟 优化：改用安全的 List 方式添加参数，完美兼容带空格的字符串
     */
    private fun buildHeavyEditCmdList(
        inputPath: String,
        outputPath: String,
        allEdits: Map<String, String>
    ): List<String> {
        val list = mutableListOf("ffmpeg", "-y", "-i", inputPath)

        val hasVideoChanges = allEdits.keys.any {
            it in setOf("resolution", "fps", "video_bit_rate", "video_codec", "video_profile", "pixel_format")
        }
        val hasAudioChanges = allEdits.keys.any {
            it in setOf("sample_rate", "audio_bit_rate", "audio_codec","audio_profile")
        }

        // 1. 视频流参数
        if (hasVideoChanges) {
            val userSelectedCodec = allEdits["video_codec"] ?: info.video_codec_type
            val videoEncoder = videoCodecMap[userSelectedCodec] ?: "libx264"
            list.add("-c:v")
            list.add(videoEncoder)
            if(videoEncoder=="libx264" || videoEncoder=="libx265"){
                list.add("-preset")
                list.add("ultrafast")
                list.add("-q:v")
                list.add("5")
            }

            val resolution = allEdits["resolution"]
            if (!resolution.isNullOrEmpty()) {
                val parts = resolution.split("x", "X", ":")
                if (parts.size == 2) {
                    list.add("-vf")
                    list.add("scale=${parts[0]}:${parts[1]}")
                }
            }

            val fps = allEdits["fps"]
            if (!fps.isNullOrEmpty() && fps != "-1.00") {
                list.add("-r")
                list.add(fps)
            }

            val videoBitrate = allEdits["video_bit_rate"]
            if (!videoBitrate.isNullOrEmpty() && videoBitrate != "-1") {
                list.add("-b:v")
                list.add("${videoBitrate}k")
            }

            val videoProfile = allEdits["video_profile"]
            if (!videoProfile.isNullOrEmpty()) {
                list.add("-profile:v")
                list.add(videoProfile)
            }

            val pixFmt = allEdits["pixel_format"]
            if (!pixFmt.isNullOrEmpty()) {
                list.add("-pix_fmt")
                list.add(pixFmt)
            }
        } else {
            list.add("-c:v")
            list.add("copy")
        }

        // 2. 音频流参数
        if (hasAudioChanges) {
            val userSelectedAudioCodec = allEdits["audio_codec"] ?: info.audio_codec_type
            val audioEncoder = audioCodecMap[userSelectedAudioCodec] ?: "aac"
            list.add("-c:a")
            list.add(audioEncoder)

            if (audioEncoder == "flac" || audioEncoder == "opus") {
                list.add("-strict")
                list.add("-2")
            }

            val sampleRate = allEdits["sample_rate"]
            if (!sampleRate.isNullOrEmpty() && sampleRate != "-1") {
                list.add("-ar")
                list.add(sampleRate)
            }

            val audioBitrate = allEdits["audio_bit_rate"]
            if (!audioBitrate.isNullOrEmpty() && audioBitrate != "-1") {
                list.add("-b:a")
                list.add("${audioBitrate}k")
            }

            // 在 hasAudioChanges 分支内追加：
            val audioProfile = allEdits["audio_profile"]
            if (!audioProfile.isNullOrEmpty()) {
                list.add("-profile:a")
                list.add(audioProfile)
            }

        } else {
            list.add("-c:a")
            list.add("copy")
        }

        // 3. 元数据处理
        val title = allEdits["title"]
        if (!title.isNullOrEmpty()) {
            list.add("-metadata")
            list.add("title=$title")
        }

        val artist = allEdits["artist"]
        if (!artist.isNullOrEmpty()) {
            list.add("-metadata")
            list.add("artist=$artist")
        }

        val comment = allEdits["comment"]
        if (!comment.isNullOrEmpty()) {
            list.add("-metadata")
            list.add("comment=$comment")
        }

        val rotation = allEdits["rotation"]
        if (!rotation.isNullOrEmpty()) {
            list.add("-metadata:s:v")
            list.add("rotate=$rotation")
        }

        list.add(outputPath)
        return list
    }

    fun submitHeavyTask(
        originalPath: String,
        newFileName: String
    ) {
        val targetFile = File(ConfigsUtils.target_dir, newFileName)

        // 🌟 直接生成无需正则表达式再次分割的参数数组
        val commandArgList = buildHeavyEditCmdList(
            inputPath = originalPath,
            outputPath = targetFile.absolutePath,
            allEdits = editedMap
        )

        val intArr = ArrayList<Int>().apply {
            add(2)
            add(commandArgList.size)
        }
        val longArr = ArrayList<Long>()
        val strArr = ArrayList<String>()

        val date = Date(System.currentTimeMillis())
        val formatter = SimpleDateFormat("yyyyMMddHHmmss", AppApplication.INSTANCE.applicationContext.resources.configuration.locales[0])
        val taskLogPath = "${ConfigsUtils.target_dir}/${AppApplication.INSTANCE.applicationContext.getString(R.string.ffmpeg_command_line)}${formatter.format(date)}.log"

        strArr.add(taskLogPath)
        strArr.addAll(commandArgList)

        val floatArr = ArrayList<Float>()
        val info = TaskInfo(
            intArr,
            longArr,
            strArr,
            floatArr
        )
        AppApplication.INSTANCE.taskRepository.startNewTask(info)

        // 重置编辑状态
        showSaveAsDialog = false
        cancelEditing()
    }
}