package com.example.audioandvideoeditor.ui.recording

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.entity.Task
import com.example.audioandvideoeditor.findActivity
import com.example.audioandvideoeditor.navigation.Destination
import com.example.audioandvideoeditor.utils.FilesUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Composable
fun RecordingScreen(
    videoPlay:(uri: String, route:String)->Unit,
    viewModel: RecordingViewModel = viewModel()
) {
// 1. 状态托管在 Screen 顶层，作为大闸控制悬浮菜单的存亡
    var showSettingsMenu by remember { mutableStateOf(false) }
    LocalContext.current
    Scaffold(
        topBar = { TopBar(onSettingsClick = { showSettingsMenu = true }) },
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
            RecordedVideoListScreen(videoPlay,viewModel,)

        }
// 3. 🌟 精准挂载：将我们刚才写好的专业级悬浮菜单挂在 Scaffold 容器内部
        // 它作为 Overlay 浮层存在，完全脱离 Column 布局流，无论里面参数怎么增删，绝对不会挤压下方的按钮和列表！
        RecordingSettingsMenu(
            expanded = showSettingsMenu,
            onDismiss = { showSettingsMenu = false },
            viewModel = viewModel
        )

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(onSettingsClick: () -> Unit) { // 🌟 接收一个纯粹的动作回调
    TopAppBar(
        title = { Text(stringResource(id = R.string.minimalist_screen_recording), fontWeight = FontWeight.Bold) },
        actions = {
            // 🌟 顶部工具栏唯一的静态新添加入口，轻量、极简
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = LocalContext.current.getString(R.string.label_floating_window_settings)
                )
            }
        }
    )
}

@Composable
fun RecordingControls(
    viewModel: RecordingViewModel
) {
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
    val myUiState by viewModel.uiState.collectAsState()
    if (myUiState.isRecording) {
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
            onClick = {
                // 🌟 核心综合判定：
                // 1. 如果用户在配置里【没有开启】悬浮球
                // 2. 并且当前手机【确实没有】悬浮窗权限
                if (!android.provider.Settings.canDrawOverlays(context)) {
                   // 🌟 规范修复：不越权操作 _uiState，而是给 ViewModel 发送标准指令
                   viewModel.showOverlayRecommendDialog(true)
                } else {
                    // 如果用户已经开启了、或者系统有权限了，完全不打扰，直接走原有录屏流程
                    startScreenCaptureRequest(context)
                }
            },
            containerColor = MaterialTheme.colorScheme .primary,
            modifier = Modifier.size(100.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(id = R.string.start), modifier = Modifier.size(50.dp))
        }
        Text(stringResource(id = R.string.start), style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 16.dp))
    }

// 🌟 精准挂载：开始录屏时的“可选悬浮球”即时推荐弹窗
    if (myUiState.showOverlayRecommendDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissRecommendDialog() },
            title = { Text(stringResource(R.string.dialog_title_floating_ball), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.dialog_msg_floating_ball)) },
            // 🌟 完美的 Fallback 退级设计：用户说不需要，直接帮他发起录屏，绝不拦截！
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onDismissRecommendDialog() // 关弹窗
                        viewModel.showOverlayRecommendDialog(false)
                        startScreenCaptureRequest(context)   // 毫无阻碍地直接发起原有录屏流程！
                    }
                ) {
                    Text("不需要,直接录屏", color = MaterialTheme.colorScheme.outline)
                }
            },
            // 用户说要，就带他去开启配置并授权
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onAcceptRecommendAndGo(context)
                    }
                ) {
                    Text(stringResource(R.string.btn_go_to_enable))
                }
            }
        )
    }

}


@Composable
private fun RecordedVideoListScreen(
    // 使用 viewModel() 确保 ViewModel 作用域正确且不会被重复创建
    videoPlay:(uri: String, route:String)->Unit,
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
            contentPadding= PaddingValues(24.dp)
        ) {
// 🌟 明确指定 key = { it.task_id }，让 Compose 拥有像素级的组件复用感知力
            items(
                items = tasksList,
                key = { task -> task.task_id }
            ) { task ->
                ShowVideoFileInfo(task, videoPlay, viewModel)
            }
        }
        if (viewModel.showDeleteDialog.value && viewModel.taskToDelete !=null) {
            // 0 = 仅删除记录, 1 = 删除记录及文件
            var deleteOption by remember { mutableStateOf(1) }
            AlertDialog(
                onDismissRequest = { viewModel.showDeleteDialog.value = false },
                title = { Text(stringResource(R.string.delete_task_title)) },
                text = {
                    Column {
                        // 提示文本
                        Text(
                            stringResource(
                                R.string.delete_task_confirm_message,
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        //类型2 ffmpege命令行 仅支持删除任务
                        if(viewModel.taskToDelete!!.type!=2){
                            // 选项1：仅删除记录
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { deleteOption = 0 }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = deleteOption == 0,
                                    onClick = { deleteOption = 0 }
                                )
                                Text(
                                    text = stringResource(R.string.delete_record_only),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            // 选项2：删除记录及文件
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { deleteOption = 1 }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = deleteOption == 1,
                                    onClick = { deleteOption = 1 }
                                )
                                Text(
                                    text = stringResource(R.string.delete_with_file),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (deleteOption == 1) MaterialTheme.colorScheme.error else Color.Unspecified
                                )
                            }
                        }
                        else{
                            deleteOption = 0
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { deleteOption = 0 }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = deleteOption == 0,
                                    onClick = {  }
                                )
                                Text(
                                    text = stringResource(R.string.delete_record_only),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // 根据选中的选项执行删除
                            viewModel.deleteTask(
                                deleteFile = (deleteOption == 1)  // 只有选项2才删文件
                            )
                            viewModel.showDeleteDialog.value = false
                        }
                    ) {
                        Text(stringResource(R.string.ok))  // 复用已有的 "确定" 字符串
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.showDeleteDialog.value= false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}
@Composable
private fun ShowVideoFileInfo(
    task: Task,
    videoPlay:(uri: String, route:String)->Unit,
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
                        thumbnailBitmap = FilesUtils.getThumbnail(context.contentResolver, Uri.parse(task.uri))
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
        Box(
            modifier = Modifier
                .width(128.dp)
                .height(128.dp)
                .background(color = Color.Black)
        ) {
            // 原有的缩略图或占位图标
            if (thumbnailBitmap != null) {
                Image(
                    bitmap = thumbnailBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_video_file_24),
                    tint = Color.Yellow,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 删除按钮（右下角）
            IconButton(
                onClick = {
                    viewModel.showDeleteDialog.value = true
                    viewModel.taskToDelete=task
                          },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)  // 与边缘保持距离
                    .size(36.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.9f),  // 👈 白色半透明底
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Delete, // 需要导入 material.icons.filled.Delete
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error,  // 👈 红色图标（或直接用 Color.Red）
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        task.file_name.apply {
            if(task.file_name.length<25) {
                Text(text = task.file_name)
            } else{
                Text(text =task.file_name.substring(0,19)+"..."+task.file_name.substring(task.file_name.length-5))
            }
        }
    }
}

@Composable
fun RecordingSettingsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    viewModel: RecordingViewModel
) {
    if (!expanded) return

    val myUiState by viewModel.uiState.collectAsState()
    val currentConfig = myUiState.selectedConfig

    // 🌟 核心改动：引入独立的临时缓冲配置，默认拷贝当前生效的真理快照
    // 这样用户的点选动作只会污染这个“草稿本”，而不会惊动 ViewModel 和底层服务
    var tmpConfig by remember { mutableStateOf(currentConfig) }

    val scrollState = rememberScrollState()
    val context=LocalContext.current
    var isOverlayPermissionGranted by remember {
        mutableStateOf(android.provider.Settings.canDrawOverlays(context))
    }
// 🌟 在 Screen 或你的配置面板 Composable 顶部埋下探测器
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // 🌟 只要页面重新可见（无论是首次进入，还是从系统设置页返回）
            if (event == Lifecycle.Event.ON_RESUME) {
                isOverlayPermissionGranted= android.provider.Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        // 🌟 1. 左侧放“取消”按钮：直接关闭，草稿本自然丢弃，原有配置毫发无损
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.outline)
            }
        },
        // 🌟 2. 右侧放“确认”按钮：只有点击它，才会“一发入魂”批量落盘并关闭
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    // 批量打包，一次性落盘生效
                    viewModel.updateRecordingConfig(tmpConfig)
                }
            ) {
                Text(stringResource(R.string.ok), fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.label_record_params),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // ==================== 1. 分辨率设置组 ====================
                Text(
                    text = stringResource(R.string.label_resolution),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                // 🌟 注意：对勾判断和赋值对象全部改为 tmpConfig（草稿本）
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(if (tmpConfig.videoWidth == 0) "✓ ${stringResource(R.string.resolution_adaptive)}" else stringResource(R.string.resolution_adaptive)) },
                    onClick = { tmpConfig = tmpConfig.copy(videoWidth = 0, videoHeight = 0) }
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(if (tmpConfig.videoWidth == 1080) "✓ 1080 x 1920" else "1080 x 1920") },
                    onClick = { tmpConfig = tmpConfig.copy(videoWidth = 1080, videoHeight = 1920) }
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(if (tmpConfig.videoWidth == 720) "✓ 720 x 1280" else "720 x 1280") },
                    onClick = { tmpConfig = tmpConfig.copy(videoWidth = 720, videoHeight = 1280) }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // ==================== 2. 帧率设置组 ====================
                Text(
                    text = "视频帧率 (FPS)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(if (tmpConfig.frameRate == 30) "✓ 30 FPS (日常/省电)" else "30 FPS (日常/省电)") },
                    onClick = { tmpConfig = tmpConfig.copy(frameRate = 30) }
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(if (tmpConfig.frameRate == 60) "✓ 60 FPS (游戏/高刷)" else "60 FPS (游戏/高刷)") },
                    onClick = { tmpConfig = tmpConfig.copy(frameRate = 60) }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // ==================== 3. 码率设置组 ====================
                Text(
                    text = "视频码率 (Bitrate)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                val mbps4 = 4 * 1024 * 1024
                val mbps5 = 5 * 1024 * 1024
                val mbps10 = 10 * 1024 * 1024
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(if (tmpConfig.bitRate == mbps4) "✓ 4 Mbps (标清)" else "4 Mbps (标清)") },
                    onClick = { tmpConfig = tmpConfig.copy(bitRate = mbps4) }
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(if (tmpConfig.bitRate == mbps5) "✓ 5 Mbps (标准)" else "5 Mbps (标准)") },
                    onClick = { tmpConfig = tmpConfig.copy(bitRate = mbps5) }
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(if (tmpConfig.bitRate == mbps10) "✓ 10 Mbps (超清)" else "10 Mbps (超清)") },
                    onClick = { tmpConfig = tmpConfig.copy(bitRate = mbps10) }
                )
// 在 RecordingSettingsMenu 的最后一个 Divider 下方追加：
                Divider(modifier = Modifier.padding(vertical = 8.dp))

// ==================== 4. 增值可选：悬浮球快捷开关 ====================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.label_desktop_floating_ball),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.desc_desktop_floating_ball),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // 🌟 绑定到我们的“草稿本”tmpConfig中。用户点按时，也只是改变草稿
                    Switch(
                        checked = isOverlayPermissionGranted,
                        onCheckedChange = { isChecked ->
//                            tmpConfig = tmpConfig.copy(isFloatingWindowEnabled = isChecked)
                            viewModel.onAcceptRecommendAndGo(context)
                        }
                    )
                }
            }
        }
    )
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
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.btn_share))
            }
        }
    }
}



