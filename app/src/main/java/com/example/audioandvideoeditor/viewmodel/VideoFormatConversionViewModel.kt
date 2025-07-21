package com.example.audioandvideoeditor.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.entity.MediaInfo
import com.example.audioandvideoeditor.services.TasksBinder

class VideoFormatConversionViewModel:ViewModel(){
        val info= MediaInfo()
        lateinit var tasksBinder: TasksBinder
        val targetFormatOptions = listOf("MP4","MKV","MOV","FLV",)//"AVI"   ,"MPEG" "TS","AVI"
        val targetFormatText= mutableStateOf("MP4")
        val changeTargetFormatFlag=mutableStateOf(false)
        val checkTargetFormatFlag=mutableStateOf(0)

        val audioFormatOptions = listOf("","AAC","MP3","FLAC","VORBIS","OPUS","AC-3",)
        var source_audio_format=""
        val audioFormatText= mutableStateOf("")
        val changeAudioFormatFlag=mutableStateOf(false)
        val checkAudioFormatFlag=mutableStateOf(0)

        val videoFormatOptions = listOf("","H.264(AVC)","H.265(HEVC)","MPEG-1","MPEG-2","MPEG-4 Part 2")
        var source_video_format=""
        val videoFormatText= mutableStateOf("")
        val changeVideoFormatFlag=mutableStateOf(false)
        val checkVideoFormatFlag=mutableStateOf(0)

        val videoResolutionOptions = listOf(
            "",
            "1920x1080",
            "1280x720",
            "720x480",
            "640x480",
            "1080x1920",
            "720x1280",
            "480x720",
            "480x720",
        )
        val videoResolutionText= mutableStateOf("")
        val changeVideoResolutionFlag=mutableStateOf(false)
        val checkVideoResolutionFlag=mutableStateOf(0)
        var video_height=-1
        var video_width=-1
        var source_height=-1
        var source_width=-1

        val videoBitRateText= mutableStateOf("")
        val changeVideoBitRateFlag=mutableStateOf(false)
        val checkVideoBitRateFlag=mutableStateOf(0)
        var video_bit_rate=-1L
        var source_video_bit_rate=-1L

        val audioBitRateText= mutableStateOf("")
        val changeAudioBitRateFlag=mutableStateOf(false)
        val checkAudioBitRateFlag=mutableStateOf(0)
        var audio_bit_rate=-1L
        var source_audio_bit_rate=-1L

        val frameRateText= mutableStateOf("")
        val changeFrameRateFlag=mutableStateOf(false)
        val checkFrameRateFlag=mutableStateOf(0)
        var frame_rate=-1
        var source_frame_rate=-1

        val sampleRateOptions = listOf(
        "",
        "8000",
        "11025",
        "22050",
        "32000",
        "44100",
        "47250",
        "48000",
        "50000",
        "96000",
        )
        val sampleRateText= mutableStateOf("")
        val changeSampleRateFlag=mutableStateOf(false)
        val checkSampleRateFlag=mutableStateOf(0)
        var sample_rate=-1
        var source_sample_rate=-1

        var editFileNameFlag = mutableStateOf(false)
        var target_name=""

        var showFileAttributeFlag= mutableStateOf(false)
    var currentVideoUri by mutableStateOf<Uri?>(null)
        private set

    private var exoPlayer: ExoPlayer? = null

    fun setVideoUri(uri: Uri?) {
        currentVideoUri = uri
    }
    var initialize_source_flag by mutableStateOf(false)
    fun initializeSource(context: android.content.Context) {
        if(currentVideoUri != null) {
            val text = tasksBinder.getAVInfo(currentVideoUri!!.path!!)
            info.initInfo(text)
            source_audio_format=info.audio_codec_type
            audioFormatText.value="${source_audio_format}(${context.getString(R.string.original)})"
            source_video_format=info.video_codec_type
            videoFormatText.value="${source_video_format}(${context.getString(R.string.original)})"
            source_height=info.height
            source_width=info.width
            video_height=info.height
            video_width=info.width
            videoResolutionText.value="${info.width}×${info.height}(${context.getString(R.string.original)})"

            source_video_bit_rate=info.video_bit_rate
            video_bit_rate=info.video_bit_rate
            videoBitRateText.value="${source_video_bit_rate/1000}(${context.getString(R.string.original)})"

            source_audio_bit_rate=info.audio_bit_rate
            audio_bit_rate=info.audio_bit_rate
            audioBitRateText.value="${source_audio_bit_rate/1000}(${context.getString(R.string.original)})"
            source_frame_rate= info.frame_rate.toInt()
            frame_rate=source_frame_rate
            frameRateText.value="${source_frame_rate}(${context.getString(R.string.original)})"

            source_sample_rate=info.sample_rate
            sampleRateText.value="${source_sample_rate}(${context.getString(R.string.original)})"

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
}