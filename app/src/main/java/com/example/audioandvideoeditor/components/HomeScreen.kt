package com.example.audioandvideoeditor.components

import PermissionsScreen
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.audioandvideoeditor.MainActivity
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.findActivity
import com.example.audioandvideoeditor.utils.AdContent
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.utils.FilesUtils
import com.example.audioandvideoeditor.utils.ImageState
import com.example.audioandvideoeditor.utils.LogUtils
import com.example.audioandvideoeditor.utils.PermissionRequestTemplate
import com.example.audioandvideoeditor.utils.observeIgnoringBatteryPermissionStatus
import com.example.audioandvideoeditor.viewmodel.AdViewModel
import com.example.audioandvideoeditor.viewmodel.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

//private fun NavHostController.navigateSingleTopTo(route: String) =
//    this.navigate(route)
//    {
//        popUpTo(
//            this@navigateSingleTopTo.graph.findStartDestination().id
//        ) {
//            saveState = true
//        }
//        launchSingleTop = true
//        restoreState = true
//    }

private fun NavHostController.navigateSingleTopTo(route: String) =
    this.navigate(route)
    {
        launchSingleTop = true
        restoreState = true

        val previousEntry = backQueue.reversed().find { it.destination.route == route }

        if (previousEntry != null) {
           //popBackStack(previousEntry.destination.route!!,false,true)
            previousEntry.destination.route?.let {
                popUpTo(it) {
                    inclusive = false // Don't remove the destination we're popping to.
                    saveState =false
                }
            }
            //Log.d(TAG,"previousEntry: ${previousEntry.destination.route}")
        }
      // Log.d(TAG,"Back Stack: ${this@navigateSingleTopTo.backQueue.map { it.destination.route }.joinToString()}")
    }

private val TAG="HomeScreen"
@Composable
fun HomeScreen(
    activity: MainActivity,
    homeViewModel: HomeViewModel= viewModel()
)
{
    val homeNavController= rememberNavController()
    val currentBackStack by homeNavController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination
    val currentScreen =HomeNavigationRow.find { it.route == currentDestination?.route }?:FunctionsCenter
    val adViewModel: AdViewModel = viewModel()
    val context= LocalContext.current
    LaunchedEffect(Unit) {
        adViewModel.preloadAd(context.getString(R.string.link),context)
    }
    Scaffold(
        bottomBar = {
            SootheBottomNavigation({ screen ->
                run {
                    homeNavController.navigateSingleTopTo(screen.route)
                }
            }, currentScreen)
        }
    )  { innerPadding ->
        NavHost(
            navController = homeNavController,
            startDestination = FunctionsCenter.route,
            modifier = Modifier.padding(innerPadding)
        ){
            composable(
                route=FunctionsCenter.route
            ){
              FunctionsCenterScreen(
                  {
                      homeNavController.navigateSingleTopTo(it)
                  },
                  {
                   homeViewModel.nextDestination={homeNavController.navigateSingleTopTo(it) }
                  })
              //Text(text="FunctionsCenterScreen")
            }
            composable(
                route=TasksCenter.route
            ){
              //  TasksCenterScreen3(task_binder,taskDao)
                TasksCenterScreen(
                    activity,
                    {path_or_uri, route,flag ->
//                        homeViewModel.file=file
                        homeViewModel.path_or_uri=path_or_uri
                        homeViewModel.route_flag=flag
                        homeNavController.navigateSingleTopTo(route)
                    }
                )
                //Text(text="TasksCenterScreen")
            }
            composable(
                route=UserCenter.route
            ){
              //  UserCenterScreen()
                UserCenterScreen {
                    homeNavController.navigateSingleTopTo(it)
                }
//                Text(text="UserCenterScreen")
            }
            composable(
                route=FileSelection.route
            ){
                FileSelectionScreen(
                    backDestination = { homeNavController .popBackStack() },
                    setFile = {
//                      homeViewModel.file=it
                        homeViewModel.path_or_uri=it.path
                    },
                    nextDestination = homeViewModel.nextDestination)
            }
            composable(
                route=ReEncoding.route
            ){
                ReEncodingScreen(activity
                                ,File(homeViewModel.path_or_uri)
                                ,{homeNavController.navigateSingleTopTo(TasksCenter.route)}
                )
            }
            composable(
                route=AudioAndVideoInfo.route
            ){
                AVInfoScreen2(activity, File(homeViewModel.path_or_uri).path)
            }
            composable(
                route=VideoFilesList.route
            ){
                VideoFilesListScreen(
                    {file, route ->
//                        homeViewModel.file=file
                        homeViewModel.path_or_uri=file.path
                        homeNavController.navigateSingleTopTo(route)
                    }
                )
            }
            composable(
                route=FFmpegInfo.route
            ){
                FFmpegInfoScreen(activity)
            }
            composable(
                route=Config.route
            ){
                ConfigScreen(
                    activity = activity,
                    nextDestination =  {
                        homeNavController.navigateSingleTopTo(it)
                    }
                    )
                //ConfigScreen2()
            }
            composable(
                route=VideoPlay.route
            ){
                VideosPlayScreen(modifier = Modifier.fillMaxSize(), path_or_uri=homeViewModel.path_or_uri)
            }
            composable(
                route=RePackaging.route
            ){
                RePackagingScreen(activity
                    ,File(homeViewModel.path_or_uri)
                    ,{homeNavController.navigateSingleTopTo(TasksCenter.route)}
                )
            }
            composable(
                route=FFmpegCommands.route
            ){
                FFmpegCommandsScreen(
                    activity,
                    {
                        homeNavController.navigateSingleTopTo(TasksCenter.route)
                        homeViewModel.show_interstistial_ad=true
                    },
                    {route:String->
                        homeNavController.navigateSingleTopTo(route)
                    },
                    {route:String->
                        homeViewModel.nextDestination={homeNavController.navigateSingleTopTo(route)}
                    },
                    File(homeViewModel.path_or_uri)
                )
            }
            composable(
                route=FilesList.route
            )
                {
                 FilesListScreen()
                }
            composable(
                route=FilesList2.route
            )
            {
                FilesListScreen2(
                    {file, route ->
//                        homeViewModel.file=file
                        homeViewModel.path_or_uri=file.path
                        homeNavController.navigateSingleTopTo(route)
                    }
                )
            }

            composable(
                route=VideoSegmenter.route
            ){
               VideoSegmenterScreen(
                    activity
                   ,File(homeViewModel.path_or_uri)
                   ,{homeNavController.navigateSingleTopTo(TasksCenter.route)}
               )
            }

            composable(
                route=PrivacyPolicy.route
            ){
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
            composable(
                route=APPInfo.route
            ){
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
            composable(
                route=ContactDeveloper.route
            ){
                val context= LocalContext.current
                val scrollState = rememberScrollState()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement= Arrangement.Center,
                    modifier = Modifier
                        .verticalScroll(scrollState) // 关键的滚动修饰符
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(stringResource(id = R.string.welcome), fontWeight = FontWeight.Bold, fontSize = 30.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text= stringResource(id = R.string.welcome_text))
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text(text= stringResource(id = R.string.link_name)+":")
                        Spacer(modifier = Modifier.width(10.dp))
                        SelectionContainer {
                            Text(
                                text= stringResource(id = R.string.link),
                                textDecoration = TextDecoration.Underline,
                                color= Color.Blue,
                                modifier = Modifier.clickable {
                                    FilesUtils.openWebLink(context,context.getString(R.string.link))
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text= stringResource(id = R.string.feedback_text))
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text(text= stringResource(id = R.string.mail_name)+":")
                        Spacer(modifier = Modifier.width(10.dp))
                        SelectionContainer {
                            Text(text = stringResource(id = R.string.mail_addr))
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text(text= stringResource(id = R.string.group_name)+":")
                        Spacer(modifier = Modifier.width(10.dp))
                        SelectionContainer {
                            Text(text = stringResource(id = R.string.group_number))
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text(text= stringResource(id = R.string.form1_name)+":")
                        Spacer(modifier = Modifier.width(10.dp))
                        SelectionContainer {
                            Text(
                                text= stringResource(id = R.string.form1_link),
                                textDecoration = TextDecoration.Underline,
                                color= Color.Blue,
                                modifier = Modifier.clickable {
                                    FilesUtils.openWebLink(context,context.getString(R.string.form1_link))
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text(text= stringResource(id = R.string.form2_name)+":")
                        Spacer(modifier = Modifier.width(10.dp))
                        SelectionContainer {
                            Text(
                                text= stringResource(id = R.string.form2_link),
                                textDecoration = TextDecoration.Underline,
                                color= Color.Blue,
                                modifier = Modifier.clickable {
                                    FilesUtils.openWebLink(context,context.getString(R.string.form2_link))
                                }
                            )
                        }
                    }
                }
            }
            composable(
                route=VideoFormatConversion.route
            ){
               VideoFormatConversionScreen(
                   activity
                   ,File(homeViewModel.path_or_uri)
                   ,{
                       homeNavController.navigateSingleTopTo(TasksCenter.route)
                       homeViewModel.show_interstistial_ad=true
                   }
               )
            }
            composable(
                route=SpeedChange.route
            ){
                SpeedChangeScreen(
                    activity
                    ,File(homeViewModel.path_or_uri)
                    ,{homeNavController.navigateSingleTopTo(TasksCenter.route)}
                )
            }
            composable(
                route=ExtractAudio.route
            ){
                ExtractAudioScreen(
                    activity
                    ,File(homeViewModel.path_or_uri)
                    ,{homeNavController.navigateSingleTopTo(TasksCenter.route)}
                )
            }

            composable(
                route=VideoMute.route
            ){
                VideoMuteScreen(
                    activity
                    , File(homeViewModel.path_or_uri)
                    ,{homeNavController.navigateSingleTopTo(TasksCenter.route)}
                )
            }
            composable(
                route=VideoAspectRatio.route
            ){
                VideoAspectRatioScreen(
                    activity
                    ,File(homeViewModel.path_or_uri)
                    ,{homeNavController.navigateSingleTopTo(TasksCenter.route)}
                )
            }
            VideoCrop
            composable(
                route=VideoCrop.route
            ){
                VideoCropScreen2(
                    activity
                    ,File(homeViewModel.path_or_uri)
                    ,{homeNavController.navigateSingleTopTo(TasksCenter.route)}
                )
 //               NineGridPage()
            }
            composable(
                route=FileRead.route
            ){
                FileReadingScreen(File(homeViewModel.path_or_uri),homeViewModel.route_flag)
            }
            composable(
                route=LogDisplay.route
            ){
                LogDisplayScreen2()
            }
            composable(
                route=APPTest.route
            ){
                APPTestScreen()
            }
            composable(
                route=Permissions.route
            ){
                PermissionsScreen()
            }
            composable(
                route=VideoCompress.route
            ){
                VideoCompressScreen(
                    activity
                    ,File(homeViewModel.path_or_uri)
                    ,{
                        homeNavController.navigateSingleTopTo(TasksCenter.route)
                        homeViewModel.show_interstistial_ad=true
                    }
                )
            }
            composable(
                route=Recording.route
            ){
                RecordingScreen({
                        uri, route->
                        homeViewModel.path_or_uri=uri
                        homeNavController.navigateSingleTopTo(route)

                })
            }
        }
        homeViewModel.show_crash_message_flag=ConfigsUtils.show_crash_message_flag
        val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        if(homeViewModel.show_crash_message_flag){
            homeViewModel.show_on_screen_ad=false
            showCrashMessage(homeViewModel)
        }
        else if(
            ConfigsUtils.gitHubRelease!=null
            &&ConfigsUtils.isNewVersionAvailable(currentVersion!!, ConfigsUtils.gitHubRelease!!.tagName)
            &&homeViewModel.showUpdateDialogFlag
        )
        {
            UpdateDialog(homeViewModel)
        }
        else if(homeViewModel.show_on_screen_ad
            && ConfigsUtils.show_on_screen_ad_again_flag){
            OnScreenAd(homeViewModel)
        }
//        val adViewModel: AdViewModel= viewModel()
//        if(!adViewModel.init_flag){
//            adViewModel.initFirebase(LocalContext.current)
//        }
//        val adUiState by adViewModel.adUiState.collectAsState()
        if(homeViewModel.show_interstistial_ad){
            ShowInterstitialAd(homeViewModel,adViewModel)
            //adViewModel.onTaskStarted()
        }
//
//        if (adUiState.showDialog) {
//            CustomAdDialog(
//                ads = adUiState.ads,
//                onAdClicked = { ad, index -> adViewModel.onAdClicked(ad, index) },
//                onDismiss = {
//                    adViewModel.onAdDismissed()
//                    homeViewModel.show_interstistial_ad =false
//                }
//            )
//        }
    }


}

@Composable
private fun OnScreenAd(
    viewModel: HomeViewModel
){
    val context= LocalContext.current
    AlertDialog(
        onDismissRequest = {
            viewModel.show_on_screen_ad=false
        },
        title ={Text(stringResource(id = R.string.welcome))},
        text ={
            val scrollState = rememberScrollState()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement= Arrangement.Center,
                modifier = Modifier
                    .verticalScroll(scrollState) // 关键的滚动修饰符
                    .fillMaxWidth()
            ) {
                Text(text= stringResource(id = R.string.welcome_text))
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(text= stringResource(id = R.string.link_name)+":")
                    Spacer(modifier = Modifier.width(10.dp))
                    SelectionContainer {
                        Text(
                            text= stringResource(id = R.string.link),
                            textDecoration = TextDecoration.Underline,
                            color= Color.Blue,
                            modifier = Modifier.clickable {
                                FilesUtils.openWebLink(context,context.getString(R.string.link))
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(text= stringResource(id = R.string.feedback_text))
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(text= stringResource(id = R.string.mail_name)+":")
                    Spacer(modifier = Modifier.width(10.dp))
                    SelectionContainer {
                        Text(text = stringResource(id = R.string.mail_addr))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(text= stringResource(id = R.string.group_name)+":")
                    Spacer(modifier = Modifier.width(10.dp))
                    SelectionContainer {
                        Text(text = stringResource(id = R.string.group_number))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(text= stringResource(id = R.string.form1_name)+":")
                    Spacer(modifier = Modifier.width(10.dp))
                    SelectionContainer {
                        Text(
                            text= stringResource(id = R.string.form1_link),
                            textDecoration = TextDecoration.Underline,
                            color= Color.Blue,
                            modifier = Modifier.clickable {
                                FilesUtils.openWebLink(context,context.getString(R.string.form1_link))
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(text= stringResource(id = R.string.form2_name)+":")
                    Spacer(modifier = Modifier.width(10.dp))
                    SelectionContainer {
                        Text(
                            text= stringResource(id = R.string.form2_link),
                            textDecoration = TextDecoration.Underline,
                            color= Color.Blue,
                            modifier = Modifier.clickable {
                                FilesUtils.openWebLink(context,context.getString(R.string.form2_link))
                            }
                        )
                    }
                }
            }
        },
        confirmButton ={
           TextButton(onClick = {
               viewModel.show_on_screen_ad=false
           }) {
               Text(stringResource(id = R.string.ok))
           }
        },
        dismissButton ={
            TextButton(onClick = {
                viewModel.show_on_screen_ad=false
                ConfigsUtils.setShowOnScreenAdAgainFlag(context, false)
            }) {
                Text(stringResource(id = R.string.dont_remind_again))
            }
        }
    )
}

@Composable
private fun AdItem(ad: AdContent, adIndex: Int) {
    val content= LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                FilesUtils.openWebLink(content, ad.clickUrl)
//                FirebaseUtils.onAdClicked(ad, adIndex)
            },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 广告图片
//            Image(
//                painter = rememberAsyncImagePainter(ad.imageUrl),
//                contentDescription = ad.title,
//                modifier = Modifier
//                    .size(96.dp)
//                    .clip(RoundedCornerShape(8.dp)),
//                contentScale = ContentScale.Crop
//            )
            NetworkImage(
                imageUrl=ad.imageUrl,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                // 广告标题
                Text(
                    text = ad.title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                // 广告描述
                Text(
                    text = ad.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ShowInterstitialAd(
    viewModel: HomeViewModel,
    adViewModel: AdViewModel
){
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    LaunchedEffect(Unit) {
        webViewInstance = adViewModel.getWebViewInstance()
    }
    val context= LocalContext.current
    AlertDialog(
        onDismissRequest = {
            viewModel.show_interstistial_ad =false
        },
        title ={
            Text(stringResource(id = R.string.task_tip))
        },
        text ={
                val isIgnoringBatteryGranted by observeIgnoringBatteryPermissionStatus()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        //.padding(16.dp)

                ) {
                    if(isIgnoringBatteryGranted){
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(id = R.string.cancel_power_saving2)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    if (webViewInstance != null) {
                        Text(
                            text = stringResource(id = R.string.ad),
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .fillMaxHeight(0.7f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                        ) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { webViewInstance!! }
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Transparent) // 设置为透明背景
                                    .clickable {
                                        FilesUtils.openWebLink(
                                            context,
                                            context.getString(R.string.link)
                                        )
                                    }
                            )
                            // 由于 Box 是可交互的，它会接收到触摸事件并拦截 WebView
                        }
                    }

                }
        },
        confirmButton ={
            TextButton(onClick = {
                viewModel.show_interstistial_ad =false
            }) {
                Text("OK")
            }
        },
        dismissButton ={}
    )
}

@Composable
fun ShowInterstitialAd_20250809(
    viewModel: HomeViewModel
){
    AlertDialog(
        onDismissRequest = {
            viewModel.show_interstistial_ad =false
        },
        title ={
            Text(stringResource(id = R.string.task_tip))
        },
        text ={},
        confirmButton ={
            TextButton(onClick = {
                viewModel.show_interstistial_ad =false
            }) {
                Text("OK")
            }
        },
        dismissButton ={}
    )
}
@Composable
fun CustomAdDialog(
    ads: List<AdContent>,
    onAdClicked: (AdContent, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Column {
            Text("这是今天的特别推荐")

            ads.forEachIndexed { index, ad ->
                // 使用 Coil 库加载网络图片
//                val painter = rememberAsyncImagePainter(ad.imageUrl)
//                Image(
//                    painter = painter,
//                    contentDescription = "Custom Ad Image",
//                    modifier = Modifier.clickable {
//                        onAdClicked(ad, index)
//                        // 点击图片后跳转到广告链接
//                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ad.clickUrl))
//                        startActivity(context, intent, null)
//                    }
//                )
                NetworkImage(
                    imageUrl=  ad.imageUrl,
                    modifier = Modifier
                        .size(200.dp)
                        .clickable {
                            onAdClicked(ad, index)
                            // 点击图片后跳转到广告链接
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ad.clickUrl))
                            startActivity(context, intent, null)
                        }
                )
                Text(ad.title)
                Text(ad.description)
            }

            Button(onClick = onDismiss) {
                Text("关闭")
            }
        }
    }
}
@Composable
fun NetworkImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    // 允许传入加载中的占位符和加载失败的图片
    placeholderResId: Int = R.drawable.refresh_24px,
    errorResId: Int = R.drawable.baseline_error_24
) {
    // 使用 remember 记住加载状态，并默认先检查缓存
    var imageState by remember {
        val cachedBitmap = FilesUtils .getCacheImage(imageUrl)
        mutableStateOf(
            if (cachedBitmap != null) {
                ImageState.Success(cachedBitmap)
            } else {
                ImageState.Loading
            }
        )
    }

    // 当图片没有缓存时，启动 LaunchedEffect 来加载
    if (imageState is ImageState.Loading) {
        LaunchedEffect(imageUrl) {
            withContext(Dispatchers.IO) {
                try {
                    val url = URL(imageUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connect()

                    val bitmap = BitmapFactory.decodeStream(connection.inputStream)
                    FilesUtils .putCacheImage(imageUrl, bitmap)

                    withContext(Dispatchers.Main) {
                        imageState = ImageState.Success(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        imageState = ImageState.Error
                    }
                }
            }
        }
    }

    // 根据 imageState 渲染 UI
    when (val state = imageState) {
        is ImageState.Loading -> {
            Image(
                painter = painterResource(id = placeholderResId),
                contentDescription = null,
                modifier = modifier
            )
        }
        is ImageState.Success -> {
            Image(
                painter = BitmapPainter(state.bitmap.asImageBitmap()),
                contentDescription = null,
                modifier = modifier
            )
        }
        is ImageState.Error -> {
            Image(
                painter = painterResource(id = errorResId),
                contentDescription = null,
                modifier = modifier
            )
        }
    }
}


/**
 * 一个封装了原生 WebView 的 Composable 函数，用于加载指定的 URL。
 * 它处理了 WebView 的生命周期、配置和资源释放。
 *
 * @param modifier 修饰符，用于调整 Composable 的布局。
 * @param url 要加载的网页 URL。
 * @param onCreated 回调，在 WebView 创建时调用，可用于自定义设置。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AdWebView(
    modifier: Modifier = Modifier,
    url: String,
    onCreated: (WebView) -> Unit = {}
) {
    val context = LocalContext.current

    // 使用 remember 缓存 WebView 实例，避免每次重组时都重新创建。
    // 在这个 lambda 中，我们创建并配置 WebView，并返回它。
    val webView = remember {
        // 创建一个 WebView 实例
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // 配置 WebView 设置
            settings.javaScriptEnabled = true // 启用 JavaScript
            // 你还可以在这里添加其他设置，例如：
            // settings.domStorageEnabled = true
            // settings.allowFileAccess = false

            // 设置 WebViewClient 来处理页面导航，防止跳出到浏览器
            webViewClient = WebViewClient()

            // 设置 WebChromeClient 来处理加载进度、标题和 JS 对话框
            webChromeClient = WebChromeClient()

            // 调用外部传入的回调函数，允许进一步自定义
            onCreated(this)
        }
    }

    // 使用 DisposableEffect 来处理 Composable 的生命周期。
    // 当 Composable 被移除时，将执行 onDispose 块中的代码。
    DisposableEffect(Unit) {
        // 当 Composable 首次进入组合时，加载 URL
        webView.loadUrl(url)

        onDispose {
            // 当 Composable 离开组合时（例如，页面被销毁），
            // 确保 WebView 停止加载、销毁，并释放所有资源。
            webView.stopLoading()
            webView.destroy()
        }
    }

    // 将配置好的 WebView 嵌入到 Compose UI 中
    AndroidView(
        modifier = modifier,
        factory = {
            // 返回我们在 remember 中创建的 WebView 实例
            webView
        }
        // 我们不再需要 update 块，因为 DisposableEffect 已经在 Composable 首次加载时处理了 URL
    )
}

@Composable
fun showCrashMessage(viewModel: HomeViewModel) {
    val context= LocalContext.current
    val message= LogUtils.getLogContext(context)
    AlertDialog(
        onDismissRequest = {
            viewModel.show_crash_message_flag=false
            ConfigsUtils.setCrashMessageFlag(context,false)
        },
        title = {
            Text(stringResource(id = R.string.crash_message_title))
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement= Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.feedback_text2))
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(text= stringResource(id = R.string.mail_name)+":")
                    Spacer(modifier = Modifier.width(10.dp))
                    SelectionContainer {
                        Text(text = stringResource(id = R.string.mail_addr))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(text= stringResource(id = R.string.group_name)+":")
                    Spacer(modifier = Modifier.width(10.dp))
                    SelectionContainer {
                        Text(text = stringResource(id = R.string.group_number))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(text= stringResource(id = R.string.form1_name)+":")
                    Spacer(modifier = Modifier.width(10.dp))
                    SelectionContainer {
                        Text(
                            text= stringResource(id = R.string.form1_link),
                            textDecoration = TextDecoration.Underline,
                            color= Color.Blue,
                            modifier = Modifier.clickable {
                                FilesUtils.openWebLink(context,context.getString(R.string.form1_link))
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(text= stringResource(id = R.string.form2_name)+":")
                    Spacer(modifier = Modifier.width(10.dp))
                    SelectionContainer {
                        Text(
                            text= stringResource(id = R.string.form2_link),
                            textDecoration = TextDecoration.Underline,
                            color= Color.Blue,
                            modifier = Modifier.clickable {
                                FilesUtils.openWebLink(context,context.getString(R.string.form2_link))
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("logs", message)
                            clipboard.setPrimaryClip(clip)
                            val toast = Toast.makeText(context, context.getString(R.string.copy_to_clipboard), Toast.LENGTH_SHORT)
                            toast.setGravity(Gravity.CENTER, 0, 0)
                            toast.show()
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_content_copy_24)
                                , contentDescription = null)
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                    ){
                        item {
                            Text(message)
                        }
                    }
                }

            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.show_crash_message_flag=false
                ConfigsUtils.setCrashMessageFlag(context,false)
            }) {
                Text("确定")
            }
        },
        dismissButton = {
        }
    )
}

@Composable
private fun UpdateDialog(
    viewModel: HomeViewModel
){
    val context= LocalContext.current
    if(viewModel.showUpdateDialogFlag) {
        AlertDialog(
            onDismissRequest = { viewModel.showUpdateDialogFlag = false },
            title = {
                Text(
                    text= stringResource(id = R.string.updates_tip),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    Text(
                        text= stringResource(id = R.string.updates_tip2)
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ){
                        Spacer(modifier = Modifier.height(20.dp))
                        SelectionContainer {
                            Text(
                                text= stringResource(id = R.string.releases_link),
                                textDecoration = TextDecoration.Underline,
                                color= Color.Blue,
                                modifier = Modifier.clickable {
                                    FilesUtils.openWebLink(context,context.getString(R.string.releases_link))
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        SelectionContainer {
                            Text(
                                text= stringResource(id = R.string.lanzout_link),
                                textDecoration = TextDecoration.Underline,
                                color= Color.Blue,
                                modifier = Modifier.clickable {
                                    FilesUtils.openWebLink(context,context.getString(R.string.lanzout_link))
                                }
                            )
                        }
                    }

                }

            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.showUpdateDialogFlag = false

                    }
                ) {
                    Text(LocalContext.current.getString(R.string.ok))
                }

            },
            dismissButton = {


            }
        )
    }
}


//if(FirebaseUtils.adContentList.isNotEmpty()) {
//    Text(
//        text = stringResource(id = R.string.ad),
//        style = MaterialTheme.typography.headlineMedium,
//        modifier = Modifier.padding(bottom = 16.dp)
//    )
//    LazyColumn {
//        items(FirebaseUtils.adContentList.size) { it ->
//            AdItem(ad = FirebaseUtils.adContentList[it], it)
//            FirebaseUtils.logAdImpression(FirebaseUtils.adContentList[it], it)
//        }
//    }
//}
//else{
////                        AdWebView(
////                            modifier = Modifier
////                                .fillMaxSize() ,
////                            url= stringResource(id = R.string.link)
////                        )
//    if (webViewInstance != null) {
//        Text(
//            text = stringResource(id = R.string.ad),
//            style = MaterialTheme.typography.headlineMedium,
//            modifier = Modifier.padding(bottom = 16.dp)
//        )
//        Box(
//            modifier = Modifier
//                .fillMaxWidth(0.9f)
//                .fillMaxHeight(0.7f)
//                .clip(RoundedCornerShape(16.dp))
//                .background(Color.White)
//        ) {
//            AndroidView(
//                modifier = Modifier.fillMaxSize(),
//                factory = { webViewInstance!! }
//            )
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(Color.Transparent) // 设置为透明背景
//                    .clickable {
//                        FilesUtils.openWebLink(
//                            context,
//                            context.getString(R.string.link)
//                        )
//                    }
//            )
//            // 由于 Box 是可交互的，它会接收到触摸事件并拦截 WebView
//        }
//    }
//}