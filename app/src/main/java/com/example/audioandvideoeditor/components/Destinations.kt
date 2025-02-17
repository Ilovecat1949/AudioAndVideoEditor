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
    VideoClipping
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

object VideoClipping:Destination{
    override val route: String
        get() = "video_clipping"
}