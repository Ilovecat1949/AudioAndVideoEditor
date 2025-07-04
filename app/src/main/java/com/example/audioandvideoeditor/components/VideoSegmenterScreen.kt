package com.example.audioandvideoeditor.components

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import com.example.audioandvideoeditor.MainActivity
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.entity.TaskInfo
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.utils.FormatsUtils
import com.example.audioandvideoeditor.utils.TextsUtils
import com.example.audioandvideoeditor.viewmodel.VideoSegmenterViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TAG="VideoClippingScreen"
@Composable
fun VideoSegmenterScreen(
    activity: MainActivity,
    file: File,
    nextDestination:()->Unit,
    videoSegmenterViewModel: VideoSegmenterViewModel= viewModel()
){
    VideoSegmenter(
        activity,
        file,
        nextDestination,
        videoSegmenterViewModel
    )
}

@Composable
fun VideoSegmenter(
    activity: MainActivity,
    file: File,
    nextDestination:()->Unit,
    viewModel: VideoSegmenterViewModel
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf(0f) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var playerView: PlayerView? = null
    var videoReady by remember { mutableStateOf(false) } // Track video readiness


    // *** IMPORTANT: Replace with your actual video URI or a test URI ***
    val sampleVideoUri = Uri.parse(file.path) // Example:  Uri.parse("android.resource://" + context.packageName + "/" + R.raw.your_video)
    if (viewModel.currentVideoUri == null) {
        viewModel.setVideoUri(sampleVideoUri)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                viewModel.initializeSource(context)

            } else if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.releasePlayer()
                videoReady = false // Reset when player is released
                viewModel.initialize_source_flag=false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            viewModel.releasePlayer()
            videoReady = false // Reset on dispose
            playerView = null //Important
        }
    }

    LaunchedEffect(viewModel.getExoPlayer()) {
        viewModel.getExoPlayer()?.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_READY) {
                    videoReady = true
//                    val currentTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
//                    viewModel.outputFileName="cropped_video_$currentTime"
                    duration = viewModel.getDuration().toFloat()
                }
            }
        })
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (viewModel.initialize_source_flag)
        { // Check videoReady!
            if(viewModel.currentVideoUri != null && videoReady) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    factory = { ctx ->
                        playerView = PlayerView(ctx).apply {
                            player = viewModel.getExoPlayer()
                            useController = true // Important: Enable the default controls
                        }
                        playerView!!
                    }
                )
            }
            else{
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                ) {
                    Text("...")
                }
            }
            Column {
                Spacer(modifier = Modifier.height(20.dp))
                Text("${context.getString(R.string.start_time)}: ${TextsUtils.millisecondsToString ((viewModel.startTime * duration ).toLong())}")
                Slider(
                    value = viewModel.startTime,
                    onValueChange = { newStartTime ->
                        viewModel.updateStartTimeAndSeek(newStartTime)
                    },
                    valueRange = 0f..1f,
                    steps = 100,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("${context.getString(R.string.end_time)}: ${TextsUtils.millisecondsToString((viewModel.endTime * duration).toLong())}")
                Slider(
                    value = viewModel.endTime,
                    onValueChange = { newEndTime ->
                        viewModel.updateEndTimeAndSeek(newEndTime)
                    },
                    valueRange = 0f..1f,
                    steps = 100,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Toggle Buttons for Cropping Mode
                ToggleButton(
                    checked = viewModel.croppingMode == VideoSegmenterViewModel.CroppingMode.Quick, // Access from ViewModel
                    onCheckedChange = { viewModel.croppingMode = VideoSegmenterViewModel.CroppingMode.Quick }, // Set in ViewModel
                    modifier = Modifier.weight(1f)
                ) {
                    Text(context.getString(R.string.quick_crop))
                }

                Spacer(modifier = Modifier.width(8.dp))

                ToggleButton(
                    checked = viewModel.croppingMode == VideoSegmenterViewModel.CroppingMode.Precise, // Access from ViewModel
                    onCheckedChange = { viewModel.croppingMode = VideoSegmenterViewModel.CroppingMode.Precise }, // Set in ViewModel
                    modifier = Modifier.weight(1f)
                ) {
                    Text(context.getString(R.string.precise_crop))
                }
            }

            Button(
                onClick = {
                    if (viewModel.endTime <= viewModel.startTime) {
                        viewModel.showTimeErrorDialog = true // Set in ViewModel
                    } else {
                        val startTimeSeconds = viewModel.startTime * duration / 1000
                        val endTimeSeconds = viewModel.endTime * duration / 1000
                        Log.d(TAG,"Start Time: $startTimeSeconds seconds, End Time: $endTimeSeconds seconds")
                        val currentTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        viewModel.outputFileName= currentTime//+".mp4"
                        viewModel.showFileNameDialog=true
                        // ... Your video clipping logic here ...
                }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(context.getString(R.string.video_duration_trimming))
            }
            // Time error dialog
            if (viewModel.showTimeErrorDialog) { // Access from ViewModel
                AlertDialog(
                    onDismissRequest = { viewModel.showTimeErrorDialog = false }, // Set in ViewModel
                    title = { Text(stringResource(id = R.string.invalid_time_selection)) },
                    text = { Text(stringResource(id = R.string.end_greater_than_start)) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.showTimeErrorDialog = false }) { // Set in ViewModel
                            Text(stringResource(id = R.string.ok))
                        }
                    }
                )
            }
            // File Name Input Dialog
            if (viewModel.showFileNameDialog) { // Access from ViewModel
                AlertDialog(
                    onDismissRequest = { viewModel.showFileNameDialog = false }, // Set in ViewModel
                    title = { Text(context.getString(R.string.file_name)) },
                    text = {
                        OutlinedTextField(
                            value = viewModel.outputFileName, // Access from ViewModel
                            onValueChange = { viewModel.outputFileName = it }, // Set in ViewModel
                            label = { Text(context.getString(R.string.file_name)) }
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (viewModel.outputFileName.isNotBlank()) {
                                val startTimeSeconds = viewModel.startTime * duration / 1000
                                val endTimeSeconds = viewModel.endTime * duration / 1000

                                Log.d(TAG,
                                    "Start Time: $startTimeSeconds seconds, End Time: $endTimeSeconds seconds, " +
                                            "Output File: ${viewModel.outputFileName}, Mode: ${viewModel.croppingMode}"
                                )
                                // Use viewModel.outputFileName in your video clipping logic
                                startTask(
                                    activity,
                                    file,
                                    nextDestination,
                                    viewModel
                                )
                                viewModel.showFileNameDialog = false // Close the dialog
                                viewModel.outputFileName = "" // Reset for next use (Optional)
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
        }
        else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
//                CircularProgressIndicator(
//                    modifier = Modifier.width(64.dp),
//                    color = MaterialTheme.colorScheme.secondary,
//                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
//                )
                Text(context.getString(R.string.wait), textAlign = TextAlign.Center) // More informative message
            }
        }
    }
}

@Composable
fun ToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Button( // Use a regular Button for better visual feedback
        onClick = { onCheckedChange(!checked) },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors( // Correct way to set colors
            containerColor  = if (checked) MaterialTheme.colorScheme .primary else Color.LightGray, // Background
            contentColor = if (checked) Color.White else Color.Black // Text color
        )
    ) {
        content()
    }
}



@Composable
private fun PlayerSurface(
    modifier: Modifier,
    onPlayerViewAvailable: (PlayerView) -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                useController =true
                onPlayerViewAvailable(this)
            }
        },
        modifier = modifier
    )
}
private fun startTask(
    activity: MainActivity,
    file: File,
    nextDestination:()->Unit,
    viewModel: VideoSegmenterViewModel
){
    val start_time=
        if(viewModel.startTime==0f){
            0L
        }
    else {
            (viewModel.startTime * viewModel.getDuration()).toLong()
        }
    val end_time=
        if(viewModel.endTime==1f){
            viewModel.getDuration()
        }
    else {
            (viewModel.endTime * viewModel.getDuration()).toLong()
        }
    val new_duration=end_time-start_time
    val target_path=ConfigsUtils.target_dir+"/${viewModel.outputFileName}.${file.extension}"
    val cmd_str=
        if(viewModel.croppingMode==VideoSegmenterViewModel.CroppingMode.Quick){
            "ffmpeg -ss ${TextsUtils.millisecondsToString(start_time)} -t ${TextsUtils.millisecondsToString(new_duration)} -i input_file  -c copy output_file"
        }
        else if(
            FormatsUtils.FullySupportsFormatsConfigs.containsKey(file.extension.uppercase())
            &&FormatsUtils.FullySupportsFormatsConfigs[file.extension.uppercase()]!!.videoCodecs.contains("H.264(AVC)")){
            "ffmpeg -ss ${TextsUtils.millisecondsToString(start_time)} -t ${TextsUtils.millisecondsToString(new_duration)} -i input_file -vcodec libx264 -preset ultrafast -q:v 5  output_file"
        }
        else
        {
            "ffmpeg -ss ${TextsUtils.millisecondsToString(start_time)} -t ${TextsUtils.millisecondsToString(new_duration)} -i input_file  output_file"
       }
    val command_arg_list=cmd_str.trim().split("[\\s\\n]+".toRegex())
        .map {
            when(it){
                "input_file"->file.path
                "output_file"->target_path
                else -> it
            }
        }
    val int_arr=ArrayList<Int>()
    int_arr.add(3)
    int_arr.add(command_arg_list.size)
    val long_arr=ArrayList<Long>()
    long_arr.add(new_duration)
    val str_arr=ArrayList<String>()
    str_arr.add(target_path)
    val date= Date(System.currentTimeMillis())
    val formatter= SimpleDateFormat("yyyyMMddHHmmss", activity.resources.configuration.locales[0])
    val task_log_path= activity.filesDir.absolutePath+"/ffmpeg"+formatter.format(date)+".log"
    str_arr.add(task_log_path)
    str_arr.addAll(command_arg_list)
    val float_arr=ArrayList<Float>()
    val info= TaskInfo(
        int_arr,
        long_arr,
        str_arr,
        float_arr
    )
    activity.tasksBinder.startTask(info)
    viewModel.outputFileName=""
    viewModel.startTime=0f
    viewModel.endTime=1f
    viewModel.setVideoUri(null)
    nextDestination()
}



//测试代码
//TextButton(onClick = {
//    val cmd_str="ffmpeg -ss 00:03:00.0 -t 00:01:00.0 -i ${Environment.getExternalStorageDirectory().path}/Download/1488047989-1-16.mp4  -c copy ${Environment.getExternalStorageDirectory().path}/Download/output.mp4"
//    val command_arg_list=cmd_str.trim().split("[\\s\\n]+".toRegex())
//    val int_arr=ArrayList<Int>()
//    int_arr.add(3)
//    int_arr.add(command_arg_list.size)
//    val long_arr=ArrayList<Long>()
//    long_arr.add(180000L)
//    long_arr.add(240000L)
//    val str_arr=ArrayList<String>()
//    str_arr.add("${Environment.getExternalStorageDirectory().path}/Download/output.mp4")
//    str_arr.addAll(command_arg_list)
//    val float_arr=ArrayList<Float>()
//    val info= TaskInfo(
//        int_arr,
//        long_arr,
//        str_arr,
//        float_arr
//    )
//    activity.tasksBinder.startTask(info)
//    nextDestination()
//}){
//    Text("确定")
//}