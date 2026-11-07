package com.example.audioandvideoeditor.ui.videoregion

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.audioandvideoeditor.application.AppApplication
import com.example.audioandvideoeditor.entity.MediaInfo
import java.io.File

class VideoRegionEditViewModel : ViewModel() {

    // 1. 区域编辑功能模式
    enum class EditMode {
        CROP,             // 画面裁剪
        WATERMARK_REMOVE, // 清除水印 (delogo)
        MOSAIC_ADD    ,    // 添加马赛克
        WATERMARK_ADD // 👈 新增：添加图片水印模式
    }

    var currentMode by mutableStateOf(EditMode.CROP)
        private set

    fun switchMode(mode: EditMode) {
        currentMode = mode
    }

    // 2. 通用矩形与区域状态（替代原本专属的 crop_ 命名）
    data class Rect(var start: Float, var top: Float, var width: Float, var height: Float)

    var videoRect by mutableStateOf(Rect(0f, 0f, 0f, 0f))
    var regionRect by mutableStateOf(Rect(0f, 0f, 0f, 0f))

    var regionStart by mutableStateOf(1f / 3f)
    var regionTop by mutableStateOf(1f / 3f)
    var regionWidth by mutableStateOf(1f / 3f)
    var regionHeight by mutableStateOf(1f / 3f)

    // 3. 媒体信息与播放器管理（继承原版逻辑）
    val info = MediaInfo()
    var currentVideoUri by mutableStateOf<Uri?>(null)
        private set

    fun setVideoUri(uri: Uri?) {
        currentVideoUri = uri
    }

    var initializeSourceFlag by mutableStateOf(false)

    private var exoPlayer: ExoPlayer? = null

    fun initializeSource(context: Context) {
        if (currentVideoUri != null) {
            val text = AppApplication.INSTANCE.taskRepository.getAVInfo(currentVideoUri!!.path!!)
            info.initInfo(text)
            videoDimensions = getNewVideoDimensions(screenWidth, screenHeight, info.width, info.height)
            videoRect = Rect(
                (screenWidth - videoDimensions.first) / 2f,
                (screenHeight - videoDimensions.second) / 2f,
                videoDimensions.first * 1f,
                videoDimensions.second * 1f
            )
            // 默认初始化框选中间 1/3 区域
            regionRect = Rect(
                videoRect.start + videoRect.width * 1 / 3,
                videoRect.top + videoRect.height * 1 / 3,
                videoRect.width * 1 / 3,
                videoRect.height * 1 / 3
            )
            regionStart = 1f / 3f
            regionTop = 1f / 3f
            regionWidth = 1f / 3f
            regionHeight = 1f / 3f
        }
        if (exoPlayer == null && currentVideoUri != null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(currentVideoUri!!)
                setMediaItem(mediaItem)
                prepare()
            }
        }
        initializeSourceFlag = true
    }

    fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    fun getExoPlayer(): ExoPlayer? = exoPlayer

    fun getDuration(): Long = exoPlayer?.duration ?: 0L

    var screenWidth: Int = 0
    var screenHeight: Int = 0
    var videoDimensions by mutableStateOf(Pair(0, 0))

    fun getNewVideoDimensions(
        width: Int,
        height: Int,
        a: Int,
        b: Int
    ): Pair<Int, Int> {
        val ratio = a * 1f / b
        val hMax = Math.min(height * 1f, width / ratio)
        val newW = ratio * hMax
        return Pair(newW.toInt(), hMax.toInt())
    }

    var editFileNameFlag = mutableStateOf(false)
    var targetName = ""

    /**
     * 获取默认水印文件（若不存在则从 assets 复制到 internal storage）
     */
    fun getDefaultWatermarkFile(context: Context): File {
        val defaultFile = File(context.filesDir, "default_watermark.png")
        if (!defaultFile.exists()) {
            try {
                context.assets.open("default_watermark.png").use { input ->
                    defaultFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return defaultFile
    }

    /**
     * 校验文件是否为有效的图片
     */
    fun isValidImageFile(filePath: String): Boolean {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, options)
        return options.outWidth > 0 && options.outHeight > 0
    }

// 在 VideoRegionEditViewModel 类中新增：

    var watermarkImageBitmap by mutableStateOf<ImageBitmap?>(null)
        private set

    /**
     * 加载水印图片文件为 Compose ImageBitmap 供 Canvas 渲染
     */
    fun loadWatermarkBitmap(watermarkFile: File) {
        try {
            if (watermarkFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(watermarkFile.path)
                watermarkImageBitmap = bitmap?.asImageBitmap()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            watermarkImageBitmap = null
        }
    }

    // 🌟 新增：水印缩放模式枚举
    enum class WatermarkScaleMode {
        FIT,      // 等比例适应，居中显示，允许留白（默认）
        STRETCH   // 强行拉伸，填满方框
    }

    var watermarkScaleMode by mutableStateOf(WatermarkScaleMode.FIT)
        private set

    fun setWatermarkScaleMode(mode: WatermarkScaleMode) {
        watermarkScaleMode = mode
    }
}

