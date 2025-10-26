package com.example.audioandvideoeditor.components

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.entity.Task
import com.example.audioandvideoeditor.findActivity
import com.example.audioandvideoeditor.utils.FilesUtils
import com.example.audioandvideoeditor.viewmodel.RecordingAudioSettings
import com.example.audioandvideoeditor.viewmodel.RecordingState
import com.example.audioandvideoeditor.viewmodel.RecordingVideoSettings
import com.example.audioandvideoeditor.viewmodel.RecordingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File


@Composable
fun RecordingScreen(
    videoPlay:(uri: String, route:String)->Unit,
    viewModel: RecordingViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
    )
) {
    viewModel.videoPlay=videoPlay
    Scaffold(
        topBar = { TopBar() },
        bottomBar = {  }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 权限检查和请求
//            PermissionCheck(uiState, viewModel)

            // 录屏控制按钮
            RecordingControls(viewModel)
            Divider(color = Color.LightGray, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(stringResource(id = R.string.recorded_videos), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            RecordedVideoListScreen(viewModel)

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar() {

    TopAppBar(
        title = { Text(stringResource(id = R.string.minimalist_screen_recording), fontWeight = FontWeight.Bold) },
    )
}

@Composable
fun RecordingControls(viewModel: RecordingViewModel) {
    val context= LocalContext.current
    // 1. 定义 ActivityResultLauncher
    //    它接收一个 Intent 作为输入，并返回一个 ActivityResult 对象
    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val activity=context.findActivity()
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // 用户同意录屏，启动录屏服务
            viewModel.startRecordingService(activity,result.resultCode, result.data!!)
            Toast.makeText(activity, activity.getString(R.string.start), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(activity, activity.getString(R.string.rejected), Toast.LENGTH_SHORT).show()
        }
    }

    // 2. 启动录屏请求的函数
    val startScreenCaptureRequest = remember<(Context) -> Unit> {
        { ctx ->
            // 获取 MediaProjectionManager
            val projectionManager = ctx.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

            // 创建屏幕捕获意图
            val intent = projectionManager.createScreenCaptureIntent()
            // 使用 Compose 启动器启动意图
            screenCaptureLauncher.launch(intent)
        }
    }
    if (viewModel.isRecording) {
        FloatingActionButton(
            onClick = { viewModel.onStopRecording(context) },
            containerColor= Color.Red,
            modifier = Modifier.size(100.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.stop), modifier = Modifier.size(50.dp))
        }
        Text(stringResource(id = R.string.recording), style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 16.dp))
    } else {
        FloatingActionButton(
            onClick = { startScreenCaptureRequest(context) },
            containerColor = MaterialTheme.colorScheme .primary,
            modifier = Modifier.size(100.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(id = R.string.start), modifier = Modifier.size(50.dp))
        }
        Text(stringResource(id = R.string.start), style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 16.dp))
    }

    // 2. 使用 DisposableEffect 来控制 ViewModel 的绑定和解绑
    DisposableEffect(Unit) {
        // Composable 进入：通知 ViewModel 绑定服务
        viewModel.onBindService(context)
        onDispose {
            // Composable 退出：通知 ViewModel 解绑服务
            // 注意：此时 ViewModel 只是执行解绑操作，但它本身可能不会被 onCleared()
            // 只有当 Activity 永久销毁且没有其他使用者时，onCleared() 才会调用
            viewModel.onUnbindService(context)
        }
    }
}


@Composable
private fun RecordedVideoListScreen(
    // 使用 viewModel() 确保 ViewModel 作用域正确且不会被重复创建
    viewModel: RecordingViewModel
) {
    // 1. 收集 StateFlow 的值，并将其转换为 Compose 的 State
    // 当 videoList 的值发生变化时，这个 Composable 会自动 Recompose
    val tasksList by viewModel.tasksListState.collectAsState()

    // 2. 根据状态显示 UI
    if (tasksList.isEmpty()) {
        Text(
            stringResource(id = R.string.empty),
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 128.dp),
        ) {
            items(tasksList) { task ->
                ShowVideoFileInfo(task,viewModel)
            }
        }
    }
}
@Composable
private fun ShowVideoFileInfo(
    task: Task,
    viewModel: RecordingViewModel
){
//    val  bitmap= FilesUtils.getThumbnail(LocalContext.current.contentResolver, Uri.parse(task.uri))
//    bitmap?.prepareToDraw()
    val context= LocalContext.current

    var thumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(task.uri) {
        withContext(Dispatchers.IO){
            viewModel.mutex.withLock {
                if(thumbnailBitmap==null){
                    val bitmap=viewModel.thumbnailBitmapArray.find { it.first==task.uri }?.second
                    if(bitmap!=null){
                        thumbnailBitmap=bitmap
                    }
                    else{
                        thumbnailBitmap = FilesUtils.getThumbnail(context.contentResolver,Uri.parse(task.uri))
                        if(thumbnailBitmap!=null){
                            if(viewModel.thumbnailBitmapArray.size>viewModel.thumbnailsMaxNum){
                                val pair=viewModel.thumbnailBitmapArray.first()
                                viewModel.thumbnailBitmapArray.removeAt(0)
                                pair.second.recycle()
                            }
                            viewModel.thumbnailBitmapArray.add(Pair(task.uri,thumbnailBitmap!!))
                        }
                    }
                    thumbnailBitmap?.prepareToDraw()
                }
            }
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement= Arrangement.Center,
    ){
        Spacer(modifier = Modifier.height(5.dp))
        if(thumbnailBitmap!=null) {
            Image(
                bitmap = thumbnailBitmap!!.asImageBitmap(),
                modifier = Modifier
                    .width(128.dp)
                    .height(128.dp)
                    .background(color = Color.Black)
                    .clickable {
                        viewModel.videoPlay(task.uri, VideoPlay.route)
                    }
                ,
                contentDescription = null
            )
        }
        else{
            Icon(painter = painterResource(id = R.drawable.baseline_video_file_24),
                tint = Color.Yellow,
                modifier = Modifier
                    .width(128.dp)
                    .height(128.dp)
                    .background(color = Color.Black)
                ,
                contentDescription = null)
        }
        Spacer(modifier = Modifier.height(5.dp))
        val file_name=FilesUtils.getFileNameUsingDocumentFile(context,Uri.parse(task.uri))
        file_name?.apply {
            if(file_name.length<25) {
                Text(text = file_name)
            }
            else{
                Text(text =file_name.substring(0,19)+"..."+file_name.substring(file_name.length-5))
            }
        }
    }
}


@Composable
fun SettingsScreen(viewModel: RecordingViewModel) {
    val videoSettings by remember { mutableStateOf(RecordingVideoSettings()) }
    val audioSettings by remember { mutableStateOf(RecordingAudioSettings()) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("视频设置", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        SettingItem(title = "分辨率", options = listOf("720p", "1080p", "2K"), selected = videoSettings.resolution) {
            // TODO: Update video settings in ViewModel
        }
        SettingItem(title = "码率", options = listOf("2 Mbps", "4 Mbps", "8 Mbps"), selected = videoSettings.bitRate) {
            // TODO: Update video settings
        }
        // ... 其他视频和音频设置
    }
}

@Composable
fun SettingItem(title: String, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.displayMedium)
        // TODO: Implement DropdownMenu for options
    }
}




@Composable
fun VideoItem(videoPath: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TODO: Display video thumbnail
            Column(modifier = Modifier.weight(1f)) {
                Text(videoPath.substringAfterLast('/'), fontWeight = FontWeight.Bold)
                // TODO: Display video duration and size
            }
            IconButton(onClick = { /* TODO: Share video */ }) {
                Icon(Icons.Default.Share, contentDescription = "分享")
            }
        }
    }
}

@Composable
fun ErrorDialog(uiState: RecordingState, viewModel: RecordingViewModel) {
    uiState.currentError?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.onDismissError() },
            title = { Text("错误") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = { viewModel.onDismissError() }) {
                    Text("确定")
                }
            }
        )
    }
}



//@Composable
//fun PermissionCheck(viewModel: RecordingViewModel) {
//    // 根据 Android 版本动态添加 WRITE_EXTERNAL_STORAGE 权限
//    val requiredPermissions = remember {
//        val permissions = mutableListOf(
//            Manifest.permission.RECORD_AUDIO,
//        )
//        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
//            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//        }
//        permissions
//    }
//
//    val permissionsState = rememberMultiplePermissionsState(permissions = requiredPermissions)
//
//    LaunchedEffect(permissionsState.allPermissionsGranted) {
//        viewModel.onPermissionsResult(permissionsState.allPermissionsGranted)
//    }
//
//    if (!permissionsState.allPermissionsGranted) {
//        val allPermissionsRevoked =
//            permissionsState.permissions.size == permissionsState.revokedPermissions.size
//
//        val textToShow = if (!allPermissionsRevoked) {
//            "为了使用录屏功能，请授予我们访问麦克风和存储的权限。"
//        } else {
//            "录屏功能需要这些权限，请在设置中授予。"
//        }
//
//        AlertDialog(
//            onDismissRequest = { /* Dismissal is handled by user action */ },
//            title = { Text("权限请求") },
//            text = { Text(textToShow) },
//            confirmButton = {
//                TextButton(
//                    onClick = {
//                        permissionsState.launchMultiplePermissionRequest()
//                    }
//                ) {
//                    Text("继续")
//                }
//            }
//        )
//    }
//}


//    val life= rememberLifecycle()
//    life.onLifeCreate {
//        mediaProjectionManager = activity.getSystemService(MediaProjectionManager::class.java)
//    }
//    startMediaProjectionLauncher=
//            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
//                    // 用户同意录屏，启动录屏服务
//                    startRecordingService(activity,result.resultCode, result.data!!)
//                    Toast.makeText(activity, "开始录制", Toast.LENGTH_SHORT).show()
//                } else {
//                    Toast.makeText(activity, "录屏权限被拒绝", Toast.LENGTH_SHORT).show()
//                }
//            }

//    val uiState by viewModel.uiState.collectAsState()


//@Composable
//fun VideoListScreen(viewModel: RecordingViewModel) {
////    val videos by viewModel.uiState.collectAsState().recordedVideos
////
////    LazyColumn {
////        items(videos) { videoPath ->
////            VideoItem(videoPath)
////        }
////    }
////
////    LaunchedEffect(Unit) {
////        viewModel.onRefreshVideos()
////    }
//}


//            val isGranted by observePermissionStatus(Manifest.permission.RECORD_AUDIO)
//            FloatingActionButton(
//                onClick = {
//                    startScreenCaptureRequest(context)
////                    if(isGranted){
//////                        requestMediaProjection()
////                        startScreenCaptureRequest(context)
////                    }
////                    else{
////                        PermissionsUtils.requestRecordAudioPermission(activity)
////                    }
//                },
//                containerColor = MaterialTheme.colorScheme .primary,
//                modifier = Modifier.size(100.dp)
//            ) {
//                Icon(Icons.Default.PlayArrow,
//                    contentDescription = stringResource(id = R.string.start_recording_screen),
//                    modifier = Modifier.size(50.dp))
//            }
// 错误信息显示
//            ErrorDialog(uiState, viewModel)