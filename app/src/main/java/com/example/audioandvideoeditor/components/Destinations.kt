package com.example.audioandvideoeditor.components

interface Destination {
    val route: String
}
val HomeNavigationRow= listOf(
    FunctionsCenter,
    TasksCenter,
    UserCenter,
    FileSelection,
    ReEncoding,
    AudioAndVideoInfo,
    VideoFilesList,
    FFmpegInfo,
    Config,
    VideoPlay,
    RePackaging,
    FFmpegCommands,
    FilesList,
    FilesList2,
    VideoSegmenter,
    PrivacyPolicy,
    APPInfo,
    VideoFormatConversion,
    SpeedChange,
    VideoAspectRatio,
    VideoCrop,
    FileRead,
    LogDisplay,
    APPTest,
    Permissions,
    ContactDeveloper,
    VideoCompress,
    Recording,
)
object FunctionsCenter:Destination{
    override val route: String
        get()="functions_center"
}
object TasksCenter:Destination{
    override val route: String
        get() ="tasks_center"
}
object UserCenter:Destination{
    override val route: String
        get() ="user_center"
}
object FileSelection:Destination{
    override val route: String
        get() = "file_selection"
}
object ReEncoding:Destination{
    override val route: String
        get() = "re_encoding"
}
object AudioAndVideoInfo:Destination{
    override val route: String
        get() = "audio_video_info"
}
object VideoFilesList:Destination{
    override val route: String
        get() = "video_files_list"
}
object FFmpegInfo:Destination{
    override val route: String
        get() = "ffmpeg_info"
}
object Config:Destination{
    override val route: String
        get() = "config"
}
object VideoPlay:Destination{
    override val route: String
        get() = "video_play"
}

object RePackaging:Destination{
    override val route: String
        get() = "re_packaging"
}
object FFmpegCommands:Destination{
    override val route: String
        get() = "ffmpeg_commands"
}
object FilesList:Destination{
    override val route: String
        get() = "fileslist"
}
object FilesList2:Destination{
    override val route: String
        get() = "fileslist2"
}

object VideoSegmenter:Destination{
    override val route: String
        get() = "video_segmenter"
}

object PrivacyPolicy:Destination{
    override val route: String
        get() = "privacy_policy"

}

object APPInfo:Destination{
    override val route: String
        get() = "app_info"
}

object VideoFormatConversion:Destination{
    override val route: String
        get() = "video_format_conversion"

}

object SpeedChange:Destination{
    override val route: String
        get() = "speed_change"
}
object ExtractAudio:Destination{
    override val route: String
        get() = "extract_audio"
}

object VideoMute:Destination{
    override val route: String
        get() = "video_mute"
}

object VideoAspectRatio:Destination{
    override val route: String
        get() = "video_aspect_ratio"
}
object VideoCrop:Destination{
    override val route: String
        get() = "video_crop"

}

object FileRead:Destination{
    override val route: String
        get() = "file_read"
}

object LogDisplay:Destination{
    override val route: String
        get() = "log_display"
}
object APPTest:Destination{
    override val route: String
        get() = "app_test"
}
object Permissions:Destination{
    override val route: String
        get() = "permissions"
}

object ContactDeveloper:Destination{
    override val route: String
        get() = "contact_developer"
}

object VideoCompress:Destination{
    override val route: String
        get() = "video_compress"
}

object Recording:Destination{
    override val route: String
        get() = "recording"
}