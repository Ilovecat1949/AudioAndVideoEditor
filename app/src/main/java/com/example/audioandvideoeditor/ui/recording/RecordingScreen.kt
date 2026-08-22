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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.entity.Task
import com.example.audioandvideoeditor.findActivity
import com.example.audioandvideoeditor.model.AudioSourceOption
import com.example.audioandvideoeditor.navigation.Destination
import com.example.audioandvideoeditor.utils.FilesUtils
import kotlinx.coroutines.Dispatchers
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
    val context = LocalContext.current

    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val activity = context.findActivity()
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.startRecordingService(activity, result.resultCode, result.data!!)
            Toast.makeText(activity, activity.getString(R.string.start), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(activity, activity.getString(R.string.rejected), Toast.LENGTH_SHORT).show()
        }
    }

    val startScreenCaptureRequest = remember<(Context) -> Unit> {
        { ctx ->
            val projectionManager = ctx.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val intent = projectionManager.createScreenCaptureIntent()
            screenCaptureLauncher.launch(intent)
        }
    }
    val myUiState by viewModel.uiState.collectAsState()
// 2. 🌟 【新增】麦克风权限请求 Launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startScreenCaptureRequest(context)
        } else {
            Toast.makeText(context, "未授予麦克风权限，无法录制音频", Toast.LENGTH_SHORT).show()
            // 权限拒绝时，可降级为静音录制，或直接拦截
        }
    }

    // 3. 🌟 【新增】启动录制的安全校验入口
    val checkAndStartRecording = {
        val needsMicPermission = myUiState.selectedConfig.audioOption == AudioSourceOption.MIC || myUiState.selectedConfig.audioOption == AudioSourceOption.MIXED
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (needsMicPermission && !hasMicPermission) {
            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        } else {
            startScreenCaptureRequest(context)
        }
    }


    // 🌟 状态分流：录制中（或已暂停）展示双控制按钮组
    if (myUiState.isSessionActive) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 暂停 / 恢复 切换按钮
                FloatingActionButton(
                    onClick = {
                        if (myUiState.isPaused) {
                            viewModel.onResumeRecording(context)
                        } else {
                            viewModel.onPauseRecording(context)
                        }
                    },
                    containerColor = if (myUiState.isPaused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (myUiState.isPaused) R.drawable.play_circle_24px else R.drawable.pause_circle_24px
                        ),
                        contentDescription = if (myUiState.isPaused) stringResource(R.string.resume) else stringResource(R.string.pause),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // 停止按钮
                FloatingActionButton(
                    onClick = { viewModel.onStopRecording(context) },
                    containerColor = Color.Red,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.stop),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Text(
                text = if (myUiState.isPaused) stringResource(id = R.string.paused) else stringResource(id = R.string.recording),
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    } else {
        FloatingActionButton(
            onClick = {
                if (!android.provider.Settings.canDrawOverlays(context)) {
                    viewModel.showOverlayRecommendDialog(true)
                } else {
                    checkAndStartRecording() // 👈 替换为带有音频权限检查的方法
                }
            },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(100.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(id = R.string.start), modifier = Modifier.size(50.dp))
        }
        Text(stringResource(id = R.string.start), style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 16.dp))
    }

    if (myUiState.showOverlayRecommendDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissRecommendDialog() },
            title = { Text(stringResource(R.string.dialog_title_floating_ball), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.dialog_msg_floating_ball)) },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onDismissRecommendDialog()
                        viewModel.showOverlayRecommendDialog(false)
                        checkAndStartRecording() // 👈 替换为带有音频权限检查的方法
                    }
                ) {
                    Text(stringResource(R.string.action_direct_record), color = MaterialTheme.colorScheme.outline)
                }
            },
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
        withContext(Dispatchers.IO) {
            // 1. 优先快查缓存（LruCache 内部同步，无需加锁，性能最高）
            val cachedBitmap = viewModel.thumbnailCache.get(task.uri)
            if (cachedBitmap != null) {
                thumbnailBitmap = cachedBitmap
                return@withContext
            }

            // 2. 未命中缓存时，使用 Mutex 避免同 URI 并发重复加载
            viewModel.mutex.withLock {
                // 双重检查：防止排队期间其他协程已完成加载
                val doubleCheckBitmap = viewModel.thumbnailCache.get(task.uri)
                if (doubleCheckBitmap != null) {
                    thumbnailBitmap = doubleCheckBitmap
                } else {
                    // 3. IO 线程解图
                    val loadedBitmap = FilesUtils.getThumbnail(
                        context.contentResolver,
                        Uri.parse(task.uri)
                    )

                    loadedBitmap?.let { bitmap ->
                        // 在 IO 线程提前完成纹理准备，避免 UI 主线程卡顿
                        bitmap.prepareToDraw()

                        // 写入 LruCache（若超过 maxNum，LruCache 会自动剔除最久未使用的项，交由 GC 回收）
                        viewModel.thumbnailCache.put(task.uri, bitmap)
                        thumbnailBitmap = bitmap
                    }
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
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable{
                            videoPlay(task.uri,Destination.VideoPlay.route)
                        }
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
                    text = { Text(if (tmpConfig.frameRate == 30) "✓ 30 FPS (${stringResource(R.string.mode_daily_power_saving)})" else "30 FPS (${stringResource(R.string.mode_daily_power_saving)})") },
                    onClick = { tmpConfig = tmpConfig.copy(frameRate = 30) }
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(if (tmpConfig.frameRate == 60) "✓ 60 FPS (${stringResource(R.string.mode_gaming_high_refresh)})" else "60 FPS (${stringResource(R.string.mode_gaming_high_refresh)})") },
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
                    text = { Text(if (tmpConfig.videoBitrate == mbps4) "✓ 4 Mbps (${stringResource(R.string.quality_sd)})" else "4 Mbps (${stringResource(R.string.quality_sd)})") },
                    onClick = { tmpConfig = tmpConfig.copy(videoBitrate = mbps4) }
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(if (tmpConfig.videoBitrate == mbps5) "✓ 5 Mbps (${stringResource(R.string.quality_standard)})" else "5 Mbps (${stringResource(R.string.quality_standard)})") },
                    onClick = { tmpConfig = tmpConfig.copy(videoBitrate = mbps5) }
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(if (tmpConfig.videoBitrate == mbps10) "✓ 10 Mbps (${stringResource(R.string.quality_uhd)})" else "10 Mbps (${stringResource(R.string.quality_uhd)})") },
                    onClick = { tmpConfig = tmpConfig.copy(videoBitrate = mbps10) }
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
// ==================== 4. 音频来源设置组 ====================
                Text(
                    text = stringResource(R.string.label_audio_source), // 或 "音频来源"
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

// 0: 静音 / 无音频
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(if (tmpConfig.audioOption == AudioSourceOption.NONE) "✓ ${stringResource(R.string.audio_source_none)}" else stringResource(R.string.audio_source_none)) },
                    onClick = { tmpConfig = tmpConfig.copy(audioOption = AudioSourceOption.NONE) }
                )

// 1: 麦克风
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(if (tmpConfig.audioOption == AudioSourceOption.MIC) "✓ ${stringResource(R.string.audio_source_mic)}" else stringResource(R.string.audio_source_mic)) },
                    onClick = { tmpConfig = tmpConfig.copy(audioOption = AudioSourceOption.MIC) }
                )

// 2: 系统/应用内部声音 (Android 10+)
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(if (tmpConfig.audioOption == AudioSourceOption.INTERNAL) "✓ ${stringResource(R.string.audio_source_internal)}" else stringResource(R.string.audio_source_internal)) },
                    onClick = { tmpConfig = tmpConfig.copy(audioOption = AudioSourceOption.INTERNAL) }
                )

// 3: 麦克风 + 系统内录
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(if (tmpConfig.audioOption == AudioSourceOption.MIXED) "✓ ${stringResource(R.string.audio_source_mic_and_internal)}" else stringResource(R.string.audio_source_mic_and_internal)) },
                    onClick = { tmpConfig = tmpConfig.copy(audioOption = AudioSourceOption.MIXED) }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))
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



