package com.example.audioandvideoeditor.components

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.example.audioandvideoeditor.MainActivity
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.entity.TaskInfo
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.viewmodel.ExtractAudioViewModel
import com.example.audioandvideoeditor.viewmodel.VideoMuteViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


private val TAG="VideoMuteScreen"
@Composable
fun VideoMuteScreen(
    activity: MainActivity,
    file: File,
    nextDestination:()->Unit,
    viewModel: VideoMuteViewModel = viewModel()
){
    VideoMuteScreen2(activity, file, nextDestination, viewModel)
}

@Composable
private fun VideoMuteScreen2(
    activity: MainActivity,
    file: File,
    nextDestination:()->Unit,
    viewModel: VideoMuteViewModel
){
    val context = LocalContext.current
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
        viewModel.getExoPlayer()?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    videoReady = true
                    duration = viewModel.getDuration().toFloat()
                }
            }
        })
    }
    if (viewModel.initialize_source_flag){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            ) {
                if(viewModel.currentVideoUri != null && videoReady) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize(),
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
                    Text("...")
                }
            }

        Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = {
                    viewModel.editFileNameFlag.value=true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.export))
            }
        }
        editFileName(activity, file, nextDestination, viewModel)
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

private fun startVideoMute(
    activity: MainActivity,
    file: File,
    nextDestination:()->Unit,
    viewModel: VideoMuteViewModel
){
    val target_path="${ConfigsUtils.target_dir}/${viewModel.target_name}.${file.extension}"
    val cmd_str="ffmpeg -i input_file -c:v copy -an output_file"
    Log.d(TAG,"cmd_str:${cmd_str}")
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
    long_arr.add(viewModel.getDuration())
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
    viewModel.setVideoUri(null)
    nextDestination()
}

@Composable
private fun editFileName(
    activity: MainActivity,
    file: File,
    nextDestination:()->Unit,
    viewModel: VideoMuteViewModel
){
    if(viewModel.editFileNameFlag.value){
        val currentTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        var text by remember { mutableStateOf(currentTime) }
        AlertDialog(
            onDismissRequest = {  viewModel.editFileNameFlag.value = false },
            title = { Text(stringResource(id = R.string.file_name)) },
            text = {
                TextField(
                    value = text,
                    onValueChange = {
                        text = it
                    },
                    label = { Text(stringResource(id = R.string.file_name)) }
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.target_name=text
                    startVideoMute(activity, file, nextDestination, viewModel)
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