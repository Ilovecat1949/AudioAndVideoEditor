package com.example.audioandvideoeditor.entity

data class MediaInfo(
    var title: String = "",
    var path: String = "",
    var artist: String = "",
    var description: String = "",
    var copyright: String = "",
    var encoder: String = "",
    var format_name: String = "",
    var file_size: Long = -1L,
    var duration: Long = -1L,
    var creation_time: String = "",
    var album: String="",
    var genre: String="",

    // 视频流信息
    var video_codec_type: String = "",
    var video_duration: Long = -1L,
    var width: Int = -1,
    var height: Int = -1,
    var video_bit_rate: Long = -1L,
    var frame_rate: Float = -1f,
    var rotation: Int = -1,
    var video_profile: String = "",
    var pixel_format: String = "",
    var color_space: String = "",
    var color_transfer: String = "",
    var sar_num: Int = -1,
    var sar_den: Int = -1,

    // 音频流信息
    var audio_codec_type: String = "",
    var audio_duration: Long = -1L,
    var audio_bit_rate: Long = -1L,
    var sample_rate: Int = -1,
    var audio_profile: String = "",
    var sample_format: String = "",
    var channels:Int=-1,
    // 字幕信息
    var has_subtitle: Boolean = false,
    var subtitle_language: String = "",
    val infoMap: HashMap<String, String> =HashMap()
) {
    fun initInfo(info:String){
        val info2=info.split('\n')
        info2.forEach {
            val info3=it.split(':', limit = 2)
            if(info3.size>1){
                infoMap[info3[0]]= info3[1]
            }
        }
// ================ 【新增】解析扩展字段（使用带兜底的 Safe 解析） ================
        infoMap["format_name"]?.let { format_name = it }
        infoMap["duration"]?.toLongOrNull()?.let { duration = it }
        file_size = infoMap["file_size"]?.toLongOrNull() ?: -1L

        title = infoMap["title"] ?: ""
        creation_time = infoMap["creation_time"] ?: ""
        has_subtitle = infoMap["has_subtitle"]?.toBoolean() ?: false
        subtitle_language = infoMap["subtitle_language"] ?: ""
        album=infoMap["album"] ?: ""
        genre=infoMap["genre"] ?: ""
// 🌟 针对现有数值字段的防护重构（防止为空或格式错引发 Crash）
        width = infoMap["width"]?.toIntOrNull() ?: -1
        height = infoMap["height"]?.toIntOrNull() ?: -1
        frame_rate = infoMap["frame_rate"]?.toFloatOrNull() ?: -1f
        video_bit_rate = infoMap["video_bit_rate"]?.toLongOrNull() ?: -1L
        video_duration = infoMap["video_duration"]?.toLongOrNull() ?: -1L
        color_space = infoMap["color_space"] ?: ""
        color_transfer = infoMap["color_transfer"] ?: ""
        sar_num = infoMap["sar_num"]?.toIntOrNull() ?: 1
        sar_den = infoMap["sar_den"]?.toIntOrNull() ?: 1
        video_profile = infoMap["video_profile"] ?: ""
        infoMap["rotation"]?.toIntOrNull()?.let { rotation = it }
        infoMap["pixel_format"]?.let { pixel_format = it }
        video_codec_type = infoMap["video_codec_type"] ?: ""

        audio_duration = infoMap["audio_duration"]?.toLongOrNull() ?: -1L
        sample_rate = infoMap["sample_rate"]?.toIntOrNull() ?: -1
        channels = infoMap["channels"]?.toIntOrNull() ?: -1
        audio_bit_rate = infoMap["audio_bit_rate"]?.toLongOrNull() ?: -1L
        // 音频扩展
        sample_format = infoMap["sample_format"] ?: ""
        audio_profile = infoMap["audio_profile"] ?: ""
        audio_codec_type = infoMap["audio_codec_type"] ?: ""
        artist = infoMap["artist"] ?: ""
        description = infoMap["description"] ?: ""
        copyright = infoMap["copyright"] ?: ""
        encoder = infoMap["encoder"] ?: ""

    }

//    var width=-1
//        private set
//    var height=-1
//        private set
//    var frame_rate=-1f
//        private set
//    var video_bit_rate=-1L
//        private set
//    var video_duration=-1L
//    private set
//    var audio_duration=-1L
//        private set
//    var sample_rate=-1
//        private set
//    var channels=-1
//        private set
//    var audio_bit_rate=-1L
//        private set
//    var audio_codec_type=""
//        private set
//    var video_codec_type=""
//        private set
//    var format_name = ""
//        private set
//    var duration = -1L
//        private set
//    var rotation = 0
//        private set
//    var pixel_format = ""
//        private set
//
//    // 容器扩展
//    var file_size = -1L
//        private set
//    // 视频专业扩展
//    var color_space = ""
//        private set
//    var color_transfer = ""
//        private set
//    var sar_num = 1
//        private set
//    var sar_den = 1
//        private set
//    var video_profile = ""
//        private set
//
//    // 音频专业扩展
//    var sample_format = ""
//        private set
//    var audio_profile = ""
//        private set
//
//    var title = ""
//        private set
//    var creation_time = ""
//        private set
//    var has_subtitle = false
//        private set
//    var subtitle_language = ""
//        private set
//
//    // 1. 声明新属性
//    var artist = ""
//        private set
//    var description = ""
//        private set
//    var copyright = ""
//        private set
//    var encoder = ""
//        private set
}