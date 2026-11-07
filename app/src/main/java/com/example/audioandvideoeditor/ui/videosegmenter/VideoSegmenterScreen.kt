package com.example.audioandvideoeditor.ui.videosegmenter

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
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
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.utils.FormatsUtils
import com.example.audioandvideoeditor.utils.TextsUtils
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TAG="VideoClippingScreen"
@Composable
fun VideoSegmenterScreen(
    file: File,
    nextDestination:()->Unit,
    videoSegmenterViewModel: VideoSegmenterViewModel= viewModel()
){
    VideoSegmenter(
        file,
        nextDestination,
        videoSegmenterViewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSegmenter(
    file: File,
    nextDestination:()->Unit,
    viewModel: VideoSegmenterViewModel
) {

// 1. 在 Screen 级作用域中准备状态与 Context
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var duration by remember { mutableStateOf(0f) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var playerView: PlayerView? = null
    var videoReady by remember { mutableStateOf(false) }

    val sampleVideoUri = Uri.parse(file.path)
    if (viewModel.currentVideoUri == null) {
        viewModel.setVideoUri(sampleVideoUri)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                viewModel.initializeSource(context)
            } else if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.releasePlayer()
                videoReady = false
                viewModel.initialize_source_flag=false
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
// 🌟 关键：必须有 Scaffold 和 snackbarHost 承载
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.padding(16.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) ,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (viewModel.initialize_source_flag) {
                if(viewModel.currentVideoUri != null && videoReady) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(64.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }

                // 🌟 1. 开始时间控制面板
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${context.getString(R.string.start_time)}: ${TextsUtils.millisecondsToString((viewModel.startTime * duration).toLong())}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Slider(
                            value = viewModel.startTime,
                            onValueChange = { newStartTime ->
                                viewModel.updateStartTimeAndSeek(newStartTime)
                            },
                            thumb = {
                                SliderDefaults.Thumb(
                                    interactionSource = remember { MutableInteractionSource() },
                                    thumbSize = DpSize(20.dp, 20.dp),
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
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        PrecisionControlRow(
                            stepText = viewModel.startTimeStepText,
                            onStepChange = { viewModel.startTimeStepText = it },
                            onLeftClick = { step -> viewModel.adjustStartTime(-step) },
                            onRightClick = { step -> viewModel.adjustStartTime(step) },
                            onInvalidInput = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.invalid_step_input)
                                    )
                                }
                            }
                        )
                    }
                }

                // 🌟 2. 结束时间控制面板
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${context.getString(R.string.end_time)}: ${TextsUtils.millisecondsToString((viewModel.endTime * duration).toLong())}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Slider(
                            value = viewModel.endTime,
                            onValueChange = { newEndTime ->
                                viewModel.updateEndTimeAndSeek(newEndTime)
                            },
                            thumb = {
                                SliderDefaults.Thumb(
                                    interactionSource = remember { MutableInteractionSource() },
                                    thumbSize = DpSize(20.dp, 20.dp),
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
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        PrecisionControlRow(
                            stepText = viewModel.endTimeStepText,
                            onStepChange = { viewModel.endTimeStepText = it },
                            onLeftClick = { step -> viewModel.adjustEndTime(-step) },
                            onRightClick = { step -> viewModel.adjustEndTime(step) },
                            onInvalidInput = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.invalid_step_input)
                                    )
                                }
                            }
                        )
                    }
                }

                // 🌟 3. 裁剪模式选择（改用单选框 RadioButton 样式，告别按钮堆砌感）
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                viewModel.croppingMode = VideoSegmenterViewModel.CroppingMode.Quick
                            }
                        ) {
                            RadioButton(
                                selected = viewModel.croppingMode == VideoSegmenterViewModel.CroppingMode.Quick,
                                onClick = { viewModel.croppingMode = VideoSegmenterViewModel.CroppingMode.Quick }
                            )
                            Text(
                                text = context.getString(R.string.quick_crop),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                viewModel.croppingMode = VideoSegmenterViewModel.CroppingMode.Precise
                            }
                        ) {
                            RadioButton(
                                selected = viewModel.croppingMode == VideoSegmenterViewModel.CroppingMode.Precise,
                                onClick = { viewModel.croppingMode = VideoSegmenterViewModel.CroppingMode.Precise }
                            )
                            Text(
                                text = context.getString(R.string.precise_crop),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // 底部主操作按钮（药丸形状）
                Button(
                    onClick = {
                        if (viewModel.endTime <= viewModel.startTime) {
                            viewModel.showTimeErrorDialog = true
                        } else {
                            val startTimeSeconds = viewModel.startTime * duration / 1000
                            val endTimeSeconds = viewModel.endTime * duration / 1000
                            Log.d(TAG,"Start Time: $startTimeSeconds seconds, End Time: $endTimeSeconds seconds")
                            val currentTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            viewModel.outputFileName= currentTime
                            viewModel.showFileNameDialog=true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = context.getString(R.string.video_duration_trimming),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                if (viewModel.showTimeErrorDialog) {
                    AlertDialog(
                        onDismissRequest = { viewModel.showTimeErrorDialog = false },
                        title = { Text(stringResource(id = R.string.invalid_time_selection)) },
                        text = { Text(stringResource(id = R.string.end_greater_than_start)) },
                        confirmButton = {
                            TextButton(onClick = { viewModel.showTimeErrorDialog = false }) {
                                Text(stringResource(id = R.string.ok))
                            }
                        }
                    )
                }

                if (viewModel.showFileNameDialog) {
                    AlertDialog(
                        onDismissRequest = { viewModel.showFileNameDialog = false },
                        title = { Text(context.getString(R.string.file_name)) },
                        text = {
                            OutlinedTextField(
                                value = viewModel.outputFileName,
                                onValueChange = { viewModel.outputFileName = it },
                                label = { Text(context.getString(R.string.file_name)) },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (viewModel.outputFileName.isNotBlank()) {
                                    startTask(context, file, nextDestination, viewModel)
                                    viewModel.showFileNameDialog = false
                                    viewModel.outputFileName = ""
                                }
                            }) {
                                Text(context.getString(R.string.ok))
                            }
                        },
                        dismissButton = {
                            Button(onClick = { viewModel.showFileNameDialog = false }) {
                                Text(context.getString(R.string.cancel))
                            }
                        }
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(64.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }

    }

}

// 🌟 精简精致的微调控制行（采用 OutlinedButton 消除笨重感）
@Composable
private fun PrecisionControlRow(
    stepText: String,
    onStepChange: (String) -> Unit,
    onLeftClick: (Float) -> Unit,
    onRightClick: (Float) -> Unit,
    onInvalidInput: () -> Unit // 🌟 最佳实践：将校验失败事件暴露给上层，由上层统一展示 Snackbar/Banner
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 减少按钮
        OutlinedButton(
            onClick = {
                val step = stepText.toFloatOrNull()
                if (step == null || step <= 0f) {
                    onInvalidInput()
                } else {
                    onLeftClick(step)
                }
            },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "◀ ${stringResource(R.string.decrease)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // 步长输入框
        OutlinedTextField(
            value = stepText,
            onValueChange = onStepChange,
            singleLine = true,
            label = { Text(stringResource(R.string.step_seconds)) },
            modifier = Modifier.weight(1.2f),
            shape = RoundedCornerShape(8.dp)
        )

        // 增加按钮
        OutlinedButton(
            onClick = {
                val step = stepText.toFloatOrNull()
                if (step == null || step <= 0f) {
                    onInvalidInput()
                } else {
                    onRightClick(step)
                }
            },
            modifier = Modifier.weight(1.2f),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "${stringResource(R.string.increase)} ▶",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun startTask(
    context: Context,
    file: File,
    nextDestination:()->Unit,
    viewModel: VideoSegmenterViewModel
){
    val start_time = if(viewModel.startTime==0f) { 0L } else { (viewModel.startTime * viewModel.getDuration()).toLong() }
    val end_time = if(viewModel.endTime==1f) { viewModel.getDuration() } else { (viewModel.endTime * viewModel.getDuration()).toLong() }
    val new_duration = end_time - start_time
    val target_path = ConfigsUtils.target_dir + "/${viewModel.outputFileName}.${file.extension}"
    val cmd_str = if(viewModel.croppingMode==VideoSegmenterViewModel.CroppingMode.Quick){
        "ffmpeg -ss ${TextsUtils.millisecondsToString(start_time)} -t ${TextsUtils.millisecondsToString(new_duration)} -i input_file  -c copy output_file"
    } else if(
        FormatsUtils.FullySupportsFormatsConfigs.containsKey(file.extension.uppercase())
        && FormatsUtils.FullySupportsFormatsConfigs[file.extension.uppercase()]!!.videoCodecs.contains("H.264(AVC)")){
        "ffmpeg -ss ${TextsUtils.millisecondsToString(start_time)} -t ${TextsUtils.millisecondsToString(new_duration)} -i input_file -vcodec libx264 -preset ultrafast -q:v 5  output_file"
    } else {
        "ffmpeg -ss ${TextsUtils.millisecondsToString(start_time)} -t ${TextsUtils.millisecondsToString(new_duration)} -i input_file  output_file"
    }

    val command_arg_list = cmd_str.trim().split("[\\s\\n]+".toRegex())
        .map {
            when(it){
                "input_file" -> file.path
                "output_file" -> target_path
                else -> it
            }
        }
    val int_arr = ArrayList<Int>()
    int_arr.add(3)
    int_arr.add(command_arg_list.size)
    val long_arr = ArrayList<Long>()
    long_arr.add(new_duration)
    val str_arr = ArrayList<String>()
    str_arr.add(target_path)
    val date = Date(System.currentTimeMillis())
    val formatter = SimpleDateFormat("yyyyMMddHHmmss", context.resources.configuration.locales[0])
    val task_log_path = context.filesDir.absolutePath + "/ffmpeg" + formatter.format(date) + ".log"
    str_arr.add(task_log_path)
    str_arr.addAll(command_arg_list)
    val float_arr = ArrayList<Float>()
    val info = TaskInfo(
        int_arr,
        long_arr,
        str_arr,
        float_arr
    )
    AppApplication.INSTANCE.taskRepository.startNewTask(info)
    viewModel.outputFileName = ""
    viewModel.startTime = 0f
    viewModel.endTime = 1f
    viewModel.setVideoUri(null)
    nextDestination()
}