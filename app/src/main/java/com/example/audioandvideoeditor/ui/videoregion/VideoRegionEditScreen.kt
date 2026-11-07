package com.example.audioandvideoeditor.ui.videoregion

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.application.AppApplication
import com.example.audioandvideoeditor.entity.TaskInfo
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.navigation.Destination
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.utils.FormatsUtils
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "VideoRegionEditScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoRegionEditScreen(
    file: File,
    nextDestination: () -> Unit,
    onNavigateToFilePicker: (route: String) -> Unit,
    onSaveReturnRoute: (route: String) -> Unit,
    watermarkFile: File?,
    viewModel: VideoRegionEditViewModel = viewModel()
) {
    val life = rememberLifecycle()
    life.onLifeCreate {}

    val context = LocalContext.current
    var duration by remember { mutableStateOf(0f) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var playerView: PlayerView? = null
    var videoReady by remember { mutableStateOf(false) }

    val sampleVideoUri = Uri.parse(file.path)
    if (viewModel.currentVideoUri == null) {
        viewModel.setVideoUri(sampleVideoUri)
    }
    val configuration = LocalConfiguration.current
    viewModel.screenWidth = configuration.screenWidthDp
    // 调整预览区高度比例，给下方的控制面板留出充裕的黄金操作空间
    viewModel.screenHeight = (configuration.screenHeightDp * 0.42f).toInt()

    val borderColor = Color.White
    val density = LocalDensity.current.density

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                viewModel.initializeSource(context)
            } else if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.releasePlayer()
                videoReady = false
                viewModel.initializeSourceFlag = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            viewModel.releasePlayer()
            videoReady = false
            playerView = null
        }
    }

    LaunchedEffect(viewModel.getExoPlayer()) {
        viewModel.getExoPlayer()?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    videoReady = true
                    duration = viewModel.getDuration().toFloat()
                }
            }
        })
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // 🌟 确定最终使用的有效水印文件（含安全校验与兜底）
    val effectiveWatermarkFile = remember(watermarkFile) {
        val defaultFile = viewModel.getDefaultWatermarkFile(context)
        if (watermarkFile != null && watermarkFile.exists()) {
            if (viewModel.isValidImageFile(watermarkFile.path)) {
                watermarkFile
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.watermark_invalid_file_tip)
                    )
                }
                defaultFile
            }
        } else {
            defaultFile
        }
    }
    // 🌟 实时加载水印 Bitmap
    LaunchedEffect(effectiveWatermarkFile) {
        viewModel.loadWatermarkBitmap(effectiveWatermarkFile)
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (viewModel.initializeSourceFlag) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 1. 黄金上部：预览播放器与框选 Canvas
                Box(
                    modifier = Modifier
                        .width(viewModel.screenWidth.dp)
                        .height(viewModel.screenHeight.dp)
                        .background(Color.Black)
                ) {
                    if (viewModel.currentVideoUri != null && videoReady) {
                        AndroidView(
                            modifier = Modifier
                                .padding(
                                    start = ((viewModel.screenWidth - viewModel.videoDimensions.first) / 2f).dp,
                                    top = ((viewModel.screenHeight - viewModel.videoDimensions.second) / 2f).dp
                                )
                                .size(
                                    viewModel.videoDimensions.first.dp,
                                    viewModel.videoDimensions.second.dp
                                ),
                            factory = { ctx ->
                                playerView = PlayerView(ctx).apply {
                                    player = viewModel.getExoPlayer()
                                    useController = true
                                }
                                playerView!!
                            }
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.width(48.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val left = viewModel.regionRect.start * density
                        val top = viewModel.regionRect.top * density
                        val width = viewModel.regionRect.width * density
                        val height = viewModel.regionRect.height * density

                        // 🌟 如果是水印添加模式，且已加载了图片位图，则在框内实时渲染图片
                        if (viewModel.currentMode == VideoRegionEditViewModel.EditMode.WATERMARK_ADD &&
                            viewModel.watermarkImageBitmap != null) {
                            val bitmap = viewModel.watermarkImageBitmap!!
                            drawImage(
                                image = bitmap,
                                dstOffset = IntOffset(left.toInt(), top.toInt()),
                                dstSize = IntSize(width.toInt(), height.toInt())
                            )
                        }

                        // 绘制外层白色的框选描边，保持操作可见性
                        drawRect(
                            color = borderColor,
                            topLeft = Offset(left, top),
                            size = Size(width, height),
                            style = Stroke(width = 2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }

                // 2. 核心操作下沉区：导出按钮紧贴预览下方，大拇指极易触达
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.editFileNameFlag.value = true }
                    ) {
                        Text(stringResource(id = R.string.export))
                    }
                }

                // 3. 中部：一级功能切换 Tab (Segmented Chips)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    VideoRegionEditViewModel.EditMode.values().forEach { mode ->
                        FilterChip(
                            selected = viewModel.currentMode == mode,
                            onClick = { viewModel.switchMode(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        VideoRegionEditViewModel.EditMode.CROP -> stringResource(R.string.mode_crop)
                                        VideoRegionEditViewModel.EditMode.WATERMARK_REMOVE -> stringResource(R.string.mode_remove_watermark)
                                        VideoRegionEditViewModel.EditMode.MOSAIC_ADD -> stringResource(R.string.mode_add_mosaic)
                                        VideoRegionEditViewModel.EditMode.WATERMARK_ADD -> stringResource(R.string.mode_add_watermark)
                                    }
                                )
                            }
                        )
                    }
                }

                // 4. 下方：动态属性面板容器 (Inspector Panel)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        item {
                            // 🌟 动态面板：根据当前模式展示专属控制项
                            when (viewModel.currentMode) {
                                VideoRegionEditViewModel.EditMode.WATERMARK_ADD -> {
                                     // 🟢 优化后的清爽居中/紧凑按钮
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                onNavigateToFilePicker(Destination.FileSelection2.route)
                                                onSaveReturnRoute(Destination.VideoRegion.route)
                                            },
                                            // 可以通过 contentPadding 让按钮显得更轻量精致
                                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                                        ) {
                                            Text(stringResource(R.string.watermark_select))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                else -> {
                                    // 其他模式暂不需要额外按钮，预留给未来扩展（如马赛克颗粒度调节等）
                                }
                            }

                            // 通用几何区域调整滑块组 (Crop, Remove, Mosaic, Watermark 均需定位)
                            RegionTransformSliders(viewModel = viewModel)
                        }
                    }

                    // 底部渐变提示遮罩
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                                    )
                                )
                            )
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.width(64.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }

    EditFileNameDialog(file, effectiveWatermarkFile, nextDestination, viewModel)
}

/**
 * 封装的几何区域调整滑块组件（左边距、上边距、宽度、高度）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegionTransformSliders(viewModel: VideoRegionEditViewModel) {
    Column {
        // 1. 左边距
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "${stringResource(id = R.string.distance_from_the_left_side)}:${"%.2f".format(viewModel.regionStart * 100)}%")
        }
        Slider(
            value = viewModel.regionStart,
            onValueChange = {
                val newRect = viewModel.regionRect.copy(
                    start = viewModel.videoRect.start + viewModel.videoRect.width * it
                )
                if (newRect.start + newRect.width <= viewModel.videoRect.start + viewModel.videoRect.width) {
                    viewModel.regionStart = it
                    viewModel.regionRect = newRect
                }
            },
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = remember { MutableInteractionSource() },
                    thumbSize = DpSize(24.dp, 24.dp),
                    colors = SliderDefaults.colors(MaterialTheme.colorScheme.primary)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    colors = SliderDefaults.colors().copy(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    ),
                    sliderState = sliderState,
                    thumbTrackGapSize = 0.dp
                )
            },
            modifier = Modifier.fillMaxWidth(),
            valueRange = 0f..1f
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 2. 上边距
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "${stringResource(id = R.string.distance_from_the_top)}:${"%.2f".format(viewModel.regionTop * 100)}%")
        }
        Slider(
            value = viewModel.regionTop,
            onValueChange = {
                val newRect = viewModel.regionRect.copy(
                    top = viewModel.videoRect.top + viewModel.videoRect.height * it
                )
                if (newRect.top + newRect.height <= viewModel.videoRect.top + viewModel.videoRect.height) {
                    viewModel.regionTop = it
                    viewModel.regionRect = newRect
                }
            },
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = remember { MutableInteractionSource() },
                    thumbSize = DpSize(24.dp, 24.dp),
                    colors = SliderDefaults.colors(MaterialTheme.colorScheme.primary)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    colors = SliderDefaults.colors().copy(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    ),
                    sliderState = sliderState,
                    thumbTrackGapSize = 0.dp
                )
            },
            modifier = Modifier.fillMaxWidth(),
            valueRange = 0f..1f
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 3. 宽度
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "${stringResource(id = R.string.width)}:${"%.2f".format(viewModel.regionWidth * 100)}%")
        }
        Slider(
            value = viewModel.regionWidth,
            onValueChange = {
                val newRect = viewModel.regionRect.copy(
                    width = viewModel.videoRect.width * it
                )
                if (newRect.start + newRect.width <= viewModel.videoRect.start + viewModel.videoRect.width) {
                    viewModel.regionWidth = it
                    viewModel.regionRect = newRect
                }
            },
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = remember { MutableInteractionSource() },
                    thumbSize = DpSize(24.dp, 24.dp),
                    colors = SliderDefaults.colors(MaterialTheme.colorScheme.primary)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    colors = SliderDefaults.colors().copy(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    ),
                    sliderState = sliderState,
                    thumbTrackGapSize = 0.dp
                )
            },
            modifier = Modifier.fillMaxWidth(),
            valueRange = 0f..1f
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 4. 高度
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "${stringResource(id = R.string.height)}:${"%.2f".format(viewModel.regionHeight * 100)}%")
        }
        Slider(
            value = viewModel.regionHeight,
            onValueChange = {
                val newRect = viewModel.regionRect.copy(
                    height = viewModel.videoRect.height * it
                )
                if (newRect.top + newRect.height <= viewModel.videoRect.top + viewModel.videoRect.height) {
                    viewModel.regionHeight = it
                    viewModel.regionRect = newRect
                }
            },
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = remember { MutableInteractionSource() },
                    thumbSize = DpSize(24.dp, 24.dp),
                    colors = SliderDefaults.colors(MaterialTheme.colorScheme.primary)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    colors = SliderDefaults.colors().copy(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    ),
                    sliderState = sliderState,
                    thumbTrackGapSize = 0.dp
                )
            },
            modifier = Modifier.fillMaxWidth(),
            valueRange = 0f..1f
        )
    }
}

@Composable
private fun EditFileNameDialog(
    file: File,
    watermarkFile: File,
    nextDestination: () -> Unit,
    viewModel: VideoRegionEditViewModel
) {
    val context = LocalContext.current
    if (viewModel.editFileNameFlag.value) {
        val currentTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        var text by remember { mutableStateOf(currentTime) }
        AlertDialog(
            onDismissRequest = { viewModel.editFileNameFlag.value = false },
            title = { Text(stringResource(id = R.string.file_name)) },
            text = {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(id = R.string.file_name)) }
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.targetName = text
                    startExportVideo(context, file, watermarkFile, nextDestination, viewModel)
                    viewModel.editFileNameFlag.value = false
                }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                Button(onClick = { viewModel.editFileNameFlag.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun startExportVideo(
    context: Context,
    file: File,
    watermarkFile: File,
    nextDestination: () -> Unit,
    viewModel: VideoRegionEditViewModel
) {
    var cmdStr = "ffmpeg -i input_file "

    if (viewModel.currentMode == VideoRegionEditViewModel.EditMode.WATERMARK_ADD) {
        cmdStr += "-i watermark_file "
    }
    val targetPath = "${ConfigsUtils.target_dir}/${viewModel.targetName}.${file.extension}"

    if (FormatsUtils.FullySupportsFormatsConfigs.containsKey(file.extension.uppercase())
        && FormatsUtils.FullySupportsFormatsConfigs[file.extension.uppercase()]!!.videoCodecs.contains("H.264(AVC)")
    ) {
        cmdStr += " -vcodec libx264 -preset ultrafast -q:v 5"
    }

    val w = (viewModel.info.width * viewModel.regionWidth).toInt()
    val h = (viewModel.info.height * viewModel.regionHeight).toInt()
    val x = (viewModel.info.width * viewModel.regionStart).toInt()
    val y = (viewModel.info.height * viewModel.regionTop).toInt()

    when (viewModel.currentMode) {
        VideoRegionEditViewModel.EditMode.CROP -> {
            cmdStr += " -vf crop=iw*${viewModel.regionWidth}:ih*${viewModel.regionHeight}:iw*${viewModel.regionStart}:ih*${viewModel.regionTop}"
        }
        VideoRegionEditViewModel.EditMode.WATERMARK_REMOVE -> {
            cmdStr += " -vf delogo=x=$x:y=$y:w=$w:h=$h:show=0"
        }
        VideoRegionEditViewModel.EditMode.MOSAIC_ADD -> {
            cmdStr += " -filter_complex [0:v]split[base][m];[m]crop=$w:$h:$x:$y,scale=iw/10:-1:flags=neighbor,scale=$w:$h:flags=neighbor[mosaic];[base][mosaic]overlay=$x:$y"
        }
        VideoRegionEditViewModel.EditMode.WATERMARK_ADD -> {
            // 🌟 核心升级：先对第二输入源（水印）进行 scale 缩放至用户框选的 w:h，再与视频流 overlay 叠加
            cmdStr += " -filter_complex [1:v]scale=$w:$h[wm_scaled];[0:v][wm_scaled]overlay=$x:$y"
        }
    }

    cmdStr += " output_file"
    Log.d(TAG, "ffmpeg cmd: $cmdStr")

    val commandArgList = cmdStr.trim().split("[\\s\\n]+".toRegex())
        .map {
            when (it) {
                "input_file" -> file.path
                "output_file" -> targetPath
                "watermark_file" -> watermarkFile.path
                else -> it
            }
        }

    val intArr = ArrayList<Int>().apply {
        add(3)
        add(commandArgList.size)
    }
    val longArr = ArrayList<Long>().apply {
        add(viewModel.info.video_duration * 1000)
    }
    val strArr = ArrayList<String>().apply {
        add(targetPath)
        val formatter = SimpleDateFormat("yyyyMMddHHmmss", context.resources.configuration.locales[0])
        val taskLogPath = context.filesDir.absolutePath + "/ffmpeg" + formatter.format(Date()) + ".log"
        add(taskLogPath)
        addAll(commandArgList)
    }
    val floatArr = ArrayList<Float>()

    val taskInfo = TaskInfo(intArr, longArr, strArr, floatArr)
    AppApplication.INSTANCE.taskRepository.startNewTask(taskInfo)
    nextDestination()
}