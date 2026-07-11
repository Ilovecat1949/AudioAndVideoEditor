package com.example.audioandvideoeditor.components

import PermissionsScreen
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState // 补充缺失的导入
import androidx.navigation.compose.rememberNavController
import com.example.audioandvideoeditor.MainActivity
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.navigation.Destination
import com.example.audioandvideoeditor.navigation.navigateSingleTopTo
import com.example.audioandvideoeditor.ui.avinfo.AVInfoScreen
import com.example.audioandvideoeditor.ui.extractaudio.ExtractAudioScreen
import com.example.audioandvideoeditor.ui.ffmpegcommand.FFmpegCommandsScreen
import com.example.audioandvideoeditor.ui.ffmpeginfo.FFmpegInfoScreen
import com.example.audioandvideoeditor.ui.formatconversion.VideoFormatConversionScreen
import com.example.audioandvideoeditor.ui.functionscenter.FunctionsCenterScreen
import com.example.audioandvideoeditor.ui.recording.RecordingScreen
import com.example.audioandvideoeditor.ui.speedchange.SpeedChangeScreen
import com.example.audioandvideoeditor.ui.taskcenter.TasksCenterScreen
import com.example.audioandvideoeditor.ui.usercenter.UserCenterScreen
import com.example.audioandvideoeditor.ui.videoaspectratio.VideoAspectRatioScreen
import com.example.audioandvideoeditor.ui.videocompress.VideoCompressScreen
import com.example.audioandvideoeditor.ui.videocrop.VideoCropScreen
import com.example.audioandvideoeditor.ui.videomute.VideoMuteScreen
import com.example.audioandvideoeditor.ui.videoplay.VideoPlayScreen
import com.example.audioandvideoeditor.ui.videosegmenter.VideoSegmenterScreen
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.viewmodel.AdViewModel
import com.example.audioandvideoeditor.viewmodel.HomeViewModel
import java.io.File // 补充File类导入（如果原有代码没导入的话）

/**
 * 主页面（优化后，职责单一：仅负责导航宿主和弹窗展示）
 */
@Composable
fun HomeScreen(
    // 🌟 接收外部传入的通知跳转信号
    initialRoute: String? = null,
    onRouteConsumed: () -> Unit = {},
    activity: MainActivity,
    homeViewModel: HomeViewModel = viewModel()
) {
    val homeNavController = rememberNavController()
    val currentDestination by homeNavController.currentBackStackEntryAsState()
    val currentScreen = Destination.entries.find { it.route == currentDestination?.destination?.route }
        ?: Destination.FunctionsCenter
    val adViewModel: AdViewModel = viewModel()
    val context = LocalContext.current

    // 预加载广告
    LaunchedEffect(Unit) {
        adViewModel.preloadAd(context.getString(R.string.link), context)


        if (!initialRoute.isNullOrEmpty()) {
            try {
                homeNavController.navigateSingleTopTo(initialRoute)
                Log.d("NotificationNav", "成功从通知栏跳转到指定路由: $initialRoute")
            } catch (e: Exception) {
                Log.e("NotificationNav", "路由跳转失败，请检查路由名称是否拼写正确", e)
            } finally {
                // 3. 🌟 闭环：无论跳转是否成功，立刻通知 Activity 把变量置空！
                // 这彻底切断了“屏幕旋转导致重复跳转”的经典 Android 恶性 Bug 链条
                onRouteConsumed()
            }
        }
    }

    // 主布局
    Scaffold(
        bottomBar = {
            SootheBottomNavigation(
                onTabSelected = { screen -> homeNavController.navigateSingleTopTo(screen.route) },
                currentScreen = currentScreen
            )
        }
    ) { innerPadding ->
        // 导航宿主
        NavHost(
            navController = homeNavController,
            startDestination = Destination.FunctionsCenter.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 功能中心
            composable(Destination.FunctionsCenter.route) {
                FunctionsCenterScreen(
                    nextDestination = { homeNavController.navigateSingleTopTo(it) },
                    setNextToNextDestination = {
//                        homeViewModel.nextDestination =
//                            { homeNavController.navigateSingleTopTo(it) }
                        homeViewModel.target_route=it
                    }
                )
            }

            // 任务中心
            composable(Destination.TasksCenter.route) {
                TasksCenterScreen(
                    { path_or_uri, route, flag ->
                        homeViewModel.path_or_uri = path_or_uri
                        homeViewModel.route_flag = flag
                        homeNavController.navigateSingleTopTo(route)
                    }
                )
            }

            // 用户中心
            composable(Destination.UserCenter.route) {
                UserCenterScreen { homeNavController.navigateSingleTopTo(it) }
            }

            // 文件选择
            composable(Destination.FileSelection.route) {
                FileSelectionScreen(
                    backDestination = { homeNavController.popBackStack() },
                    setFile = { homeViewModel.path_or_uri = it.path },
                    nextDestination =
//                        homeViewModel.nextDestination
                        {
                            // 核心优化点：文件选完了，直接在顶层读取刚才存的标志位
                            val nextRoute = homeViewModel.target_route
                            if (nextRoute.isNotEmpty()) {
                                // 精准去往目标剪辑页面（如视频裁剪页），完全不需要借助任何复杂闭包！
                                homeNavController.navigateSingleTopTo(nextRoute)
                                // 消费掉该标志位，防止重复跳转
//                                homeViewModel.target_route = ""
                            }
                        }
                )
            }

            // 其他页面（仅保留结构，具体实现不变）
            composable(Destination.ReEncoding.route) {
                ReEncodingScreen(
                    file = File(homeViewModel.path_or_uri),
                    nextDestination = { homeNavController.navigateSingleTopTo(Destination.TasksCenter.route) }
                )
            }

            composable(Destination.AudioAndVideoInfo.route) {
                AVInfoScreen(File(homeViewModel.path_or_uri).path)
            }

            composable(Destination.VideoFilesList.route) {
                VideoFilesListScreen (
                    { file, route ->
                        homeViewModel.path_or_uri = file.path
                        homeNavController.navigateSingleTopTo(route)
                    }
                )
            }

            composable(Destination.FFmpegInfo.route) {
                FFmpegInfoScreen()
            }

            composable(Destination.Config.route) {
                ConfigScreen(
                    activity = activity,
                    nextDestination = { homeNavController.navigateSingleTopTo(it) }
                )
            }

            composable(Destination.VideoPlay.route) {
                VideoPlayScreen(
                    modifier = Modifier.fillMaxSize(),
                    path_or_uri = homeViewModel.path_or_uri
                )
            }

            composable(Destination.RePackaging.route) {
                RePackagingScreen(
                    activity = activity,
                    file = File(homeViewModel.path_or_uri),
                    nextDestination = { homeNavController.navigateSingleTopTo(Destination.TasksCenter.route) }
                )
            }

            composable(Destination.FFmpegCommands.route) {
                FFmpegCommandsScreen(
                    nextDestination = {
                        homeNavController.navigateSingleTopTo(Destination.TasksCenter.route)
                        homeViewModel.show_interstistial_ad = true
                    },
                    goDestination = { homeNavController.navigateSingleTopTo(it) },
                    setNextToNextDestination = {
//                        homeViewModel.nextDestination =
//                            { homeNavController.navigateSingleTopTo(it) }
                        homeViewModel.target_route=it
                    },
                    file = File(homeViewModel.path_or_uri)
                )
            }

            composable(Destination.FilesList.route) {
                FilesListScreen()
            }

            composable(Destination.FilesList2.route) {
                FilesListScreen2 ({ file, route ->
                    homeViewModel.path_or_uri = file.path
                    homeNavController.navigateSingleTopTo(route)
                }
                )
            }

            composable(Destination.VideoSegmenter.route) {
                VideoSegmenterScreen(
                    file = File(homeViewModel.path_or_uri),
                    nextDestination = { homeNavController.navigateSingleTopTo(Destination.TasksCenter.route) }
                )
            }

            composable(Destination.PrivacyPolicy.route) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(scrollState)
                    ,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ){
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(stringResource(id = R.string.privacy_policy), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(stringResource(id = R.string.privacy_policy_context))
                }
            }

            composable(Destination.APPInfo.route) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                    ,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ){
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(stringResource(id = R.string.app_name), fontWeight = FontWeight.Bold, fontSize = 30.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(stringResource(id = R.string.app_info_context))
                }
            }

            composable(Destination.ContactDeveloper.route) {
                ContactDeveloperScreen()
            }

            composable(Destination.VideoFormatConversion.route) {
                VideoFormatConversionScreen(
                    file = File(homeViewModel.path_or_uri),
                    nextDestination = {
                        homeNavController.navigateSingleTopTo(Destination.TasksCenter.route)
                        homeViewModel.show_interstistial_ad = true
                    }
                )
            }

            composable(Destination.SpeedChange.route) {
                SpeedChangeScreen(
                    file = File(homeViewModel.path_or_uri),
                    nextDestination = { homeNavController.navigateSingleTopTo(Destination.TasksCenter.route) }
                )
            }

            composable(Destination.ExtractAudio.route) {
                ExtractAudioScreen(
                    file = File(homeViewModel.path_or_uri),
                    nextDestination = { homeNavController.navigateSingleTopTo(Destination.TasksCenter.route) }
                )
            }

            composable(Destination.VideoMute.route) {
                VideoMuteScreen(
                    file = File(homeViewModel.path_or_uri),
                    nextDestination = { homeNavController.navigateSingleTopTo(Destination.TasksCenter.route) }
                )
            }

            composable(Destination.VideoAspectRatio.route) {
                VideoAspectRatioScreen(
                    file = File(homeViewModel.path_or_uri),
                    nextDestination = { homeNavController.navigateSingleTopTo(Destination.TasksCenter.route) }
                )
            }

            composable(Destination.VideoCrop.route) {
                VideoCropScreen(
                    file = File(homeViewModel.path_or_uri),
                    nextDestination = { homeNavController.navigateSingleTopTo(Destination.TasksCenter.route) }
                )
            }

            composable(Destination.FileRead.route) {
                FileReadingScreen(File(homeViewModel.path_or_uri), homeViewModel.route_flag)
            }

            composable(Destination.LogDisplay.route) {
                LogDisplayScreen2()
            }

            composable(Destination.APPTest.route) {
                APPTestScreen()
            }

            composable(Destination.Permissions.route) {
                PermissionsScreen()
            }

            composable(Destination.VideoCompress.route) {
                VideoCompressScreen(
                    file = File(homeViewModel.path_or_uri),
                    nextDestination = {
                        homeNavController.navigateSingleTopTo(Destination.TasksCenter.route)
                        homeViewModel.show_interstistial_ad = true
                    }
                )
            }

            composable(Destination.Recording.route) {
                RecordingScreen({ uri, route ->
                    homeViewModel.path_or_uri = uri
                    homeNavController.navigateSingleTopTo(route)
                }
                )
//                RecordingScreenTest()
            }
        }

        // 弹窗展示逻辑
        homeViewModel.show_crash_message_flag = ConfigsUtils.show_crash_message_flag
        val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName

        // 崩溃日志弹窗
        if (homeViewModel.show_crash_message_flag) {
            homeViewModel.show_on_screen_ad = false
            CrashMessageDialog(
                viewModel = homeViewModel,
                onDismiss = {
                    homeViewModel.show_crash_message_flag = false
                    ConfigsUtils.setCrashMessageFlag(context, false)
                }
            )
        }
        // 版本更新弹窗
        else if (
            ConfigsUtils.gitHubRelease != null &&
            ConfigsUtils.isNewVersionAvailable(currentVersion!!, ConfigsUtils.gitHubRelease!!.tagName) &&
            homeViewModel.showUpdateDialogFlag
        ) {
            UpdateDialog(
                viewModel = homeViewModel,
                onDismiss = { homeViewModel.showUpdateDialogFlag = false }
            )
        }
        // 屏幕内广告弹窗
        else if (homeViewModel.show_on_screen_ad && ConfigsUtils.show_on_screen_ad_again_flag) {
            OnScreenAdDialog(
                viewModel = homeViewModel,
                onDismiss = { homeViewModel.show_on_screen_ad = false },
                onDontRemindAgain = { ConfigsUtils.setShowOnScreenAdAgainFlag(context, false) }
            )
        }
        // 插屏广告弹窗
        else if (homeViewModel.show_interstistial_ad) {
            InterstitialAdDialog(
                viewModel = homeViewModel,
                adViewModel = adViewModel,
                onDismiss = { homeViewModel.show_interstistial_ad = false }
            )
        }
    }
}