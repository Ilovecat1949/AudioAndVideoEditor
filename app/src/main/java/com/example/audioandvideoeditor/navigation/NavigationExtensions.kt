package com.example.audioandvideoeditor.navigation

import androidx.navigation.NavHostController

private const val TAG = "NavigationExtensions"

/**
 * 封装单一顶部导航逻辑，避免重复代码
 * @param route 目标页面路由
 */
fun NavHostController.navigateSingleTopTo(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true

        val previousEntry = backQueue.reversed().find { it.destination.route == route }
        previousEntry?.destination?.route?.let {
            popUpTo(it) {
                inclusive = false
                saveState = false
            }
        }
    }
}

/**
 * 定义导航目标密封类（补充原代码缺失的Destination定义）
 */
enum class Destination(val route: String) {
    FunctionsCenter("functions_center"),
    TasksCenter("tasks_center"),
    UserCenter("user_center"),
    FilesList2("files_list_2"),
    Config("config"),
    FileSelection("file_selection"),
    ReEncoding("re_encoding"),
    AudioAndVideoInfo("audio_and_video_info"),
    VideoFilesList("video_files_list"),
    FFmpegInfo("ffmpeg_info"),
    VideoPlay("video_play"),
    RePackaging("re_packaging"),
    FFmpegCommands("ffmpeg_commands"),
    FilesList("files_list"),
    VideoSegmenter("video_segmenter"),
    PrivacyPolicy("privacy_policy"),
    APPInfo("app_info"),
    ContactDeveloper("contact_developer"),
    VideoFormatConversion("video_format_conversion"),
    SpeedChange("speed_change"),
    ExtractAudio("extract_audio"),
    VideoMute("video_mute"),
    VideoAspectRatio("video_aspect_ratio"),
    VideoCrop("video_crop"),
    FileRead("file_read"),
    LogDisplay("log_display"),
    APPTest("app_test"),
    Permissions("permissions"),
    VideoCompress("video_compress"),
    Recording("recording");

    // 扩展：导航跳转
    fun navigate(navController: NavHostController) {
        navController.navigateSingleTopTo(this.route)
    }
}

/**
 * 查找当前导航目标
 */
val NavHostController.currentDestination: Destination?
    get() = Destination.entries.find { it.route == currentBackStackEntry?.destination?.route }