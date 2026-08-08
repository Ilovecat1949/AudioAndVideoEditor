package com.example.audioandvideoeditor.ui.speedchange

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.audioandvideoeditor.application.AppApplication
import com.example.audioandvideoeditor.entity.MediaInfo

class SpeedChangeViewModel: ViewModel()  {

    var currentVideoUri by mutableStateOf<Uri?>(null)
        private set
    val info= MediaInfo()
    private var exoPlayer: ExoPlayer? = null
    var initialize_source_flag by mutableStateOf(false)
    fun initializeSource(context: Context){
        if(currentVideoUri != null) {
            val text = AppApplication.Companion.INSTANCE.taskRepository.getAVInfo(currentVideoUri!!.path!!)
            info.initInfo(text)

        }
        if (exoPlayer == null && currentVideoUri != null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(currentVideoUri!!)
                setMediaItem(mediaItem)
                prepare()
            }
        }
        initialize_source_flag=true
    }
    fun setVideoUri(uri: Uri?) {
        currentVideoUri = uri
    }
    fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    fun getExoPlayer(): ExoPlayer? {
        return exoPlayer
    }

    fun getDuration(): Long{
        return exoPlayer?.duration?: 0L
    }

    var speed_rate=1.5f
    var editFileNameFlag = mutableStateOf(false)
    var target_name=""

    val targetFormatOptions = listOf("MP4","FLV","TS","MKV","MOV")//"AVI"   ,"MPEG"
    val targetFormatText= mutableStateOf("MP4")
    val changeTargetFormatFlag= mutableStateOf(false)
    val checkTargetFormatFlag= mutableStateOf(0)
    /**
     * 动态构建 FFmpeg 音频 atempo 滤镜字符串
     * 支持突破 0.5~2.0 的限制（支持 0.5 ~ 10.0 甚至更宽广的范围）
     */
    fun buildAudioAtempoFilter(speedRate: Float): String {
        var rate = speedRate
        val filters = mutableListOf<String>()

        if (rate > 2.0f) {
            // 大于 2.0 时，按 2.0 拆分串联
            while (rate > 2.0f) {
                filters.add("atempo=2.0")
                rate /= 2.0f
            }
            filters.add(String.format(java.util.Locale.US, "atempo=%.2f", rate))
        } else if (rate < 0.5f) {
            // 小于 0.5 时，按 0.5 拆分串联
            while (rate < 0.5f) {
                filters.add("atempo=0.5")
                rate /= 0.5f
            }
            filters.add(String.format(java.util.Locale.US, "atempo=%.2f", rate))
        } else {
            // 在 0.5 ~ 2.0 正常范围内
            filters.add(String.format(java.util.Locale.US, "atempo=%.2f", rate))
        }

        // 用逗号连接多个 atempo，例如: "atempo=2.0,atempo=2.0,atempo=2.5"
        return filters.joinToString(",")
    }
}