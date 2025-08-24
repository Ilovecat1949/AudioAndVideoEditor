package com.example.audioandvideoeditor.components

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import com.example.audioandvideoeditor.MainActivity
import com.example.audioandvideoeditor.viewmodel.VideoCompressViewModel
import java.io.File
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.entity.TaskInfo
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.utils.FormatsUtils
import com.example.audioandvideoeditor.viewmodel.VideoFormatConversionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VideoCompressScreen(
    activity: MainActivity,
    file: File,
    nextDestination:()->Unit,
    viewModel: VideoCompressViewModel = viewModel()
){
    val life= rememberLifecycle()
    life.onLifeCreate {
        viewModel.tasksBinder=activity.tasksBinder
        viewModel.videoSize=file.length()
    }
    val context = LocalContext.current
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
                    .height(200.dp)
            ){
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
            Spacer(modifier = Modifier.height(32.dp))
            var selectedOption by remember { mutableStateOf<Triple<Double,Pair<Int,Int>,String>?>(null) }
            Button(
                onClick = {
                    selectedOption?.let {
                        //Toast.makeText(context, "You selected: $it", Toast.LENGTH_SHORT).show()
                        // Here you would typically perform an action with the selected option
                        // e.g., navigate to another screen, save to database, etc.
                        viewModel.editFileNameFlag.value=true
                        viewModel.option=selectedOption!!
                    } ?: run {
                        Toast.makeText(context, context.getString(R.string.select_tip), Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = selectedOption != null, // Button is enabled only when an option is selected
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(id = R.string.export), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(32.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ){
               item {
                   viewModel.compressionOptions .forEach { option ->
                       Card(
                           modifier = Modifier
                               .fillMaxWidth()
                               .padding(vertical = 8.dp)
                               .clickable { selectedOption = option },
                           shape = RoundedCornerShape(12.dp),
                           border = BorderStroke(
                               width = 2.dp,
                               color = if (selectedOption == option) MaterialTheme.colorScheme.primary else Color.LightGray
                           ),
                           colors = CardDefaults.cardColors(
                               containerColor = if (selectedOption == option) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                           ),
                           elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                       ) {
                           Row(
                               modifier = Modifier
                                   .padding(16.dp)
                                   .fillMaxWidth(),
                               verticalAlignment = Alignment.CenterVertically
                           ) {
                               RadioButton(
                                   selected = (selectedOption == option),
                                   onClick = { selectedOption = option },
                                   colors = RadioButtonDefaults.colors(
                                       selectedColor = MaterialTheme.colorScheme.primary
                                   )
                               )
                               Spacer(modifier = Modifier.width(16.dp))
                               Text(
                                   text =String.format("%.0f", option.first * 100) + "%",
                                   style = MaterialTheme.typography.bodyLarge,
                                   color = if (selectedOption == option) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                               )
                               Spacer(modifier = Modifier.width(16.dp))
                               Spacer(modifier = Modifier.width(16.dp))
                               Text(
                                   text = "${option.second.first}x${option.second.second}",
                                   style = MaterialTheme.typography.bodyLarge,
                                   color = if (selectedOption == option) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                               )
                               Spacer(modifier = Modifier.width(16.dp))
                               Spacer(modifier = Modifier.width(16.dp))
                               Text(
                                   text = option.third,
                                   style = MaterialTheme.typography.bodyLarge,
                                   color = if (selectedOption == option) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                               )
                               Spacer(modifier = Modifier.width(16.dp))
                           }
                       }
                   }
               }
            }
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
    editFileName(activity, file, nextDestination, viewModel)
}

@Composable
private fun editFileName(
    activity: MainActivity,
    file: File,
    nextDestination:()->Unit,
    viewModel: VideoCompressViewModel
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
                    start(activity, file, nextDestination, viewModel)
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

private fun start(
    activity: MainActivity,
    file: File,
    nextDestination:()->Unit,
    viewModel: VideoCompressViewModel
){
    var cmd_str="ffmpeg -i input_file "//ffmpeg -i ${file.path}
    cmd_str=cmd_str+"-vcodec libx264 -preset ultrafast -q:v 5 "
    cmd_str=cmd_str+"-vf scale=${viewModel.option.second.first}x${viewModel.option.second.second} "
    if(viewModel.info.video_bit_rate!=-1L){
        cmd_str=cmd_str+"-b:v ${(viewModel.info.video_bit_rate*viewModel.option.first).toLong()} "
    }
    if(viewModel.info.audio_bit_rate!=-1L){
        cmd_str=cmd_str+"-b:a ${(viewModel.info.audio_bit_rate*viewModel.option.first).toLong()} "
    }
    val target_path="${ConfigsUtils.target_dir}/${viewModel.target_name}.mp4"
    cmd_str += "output_file "
//    Log.d(TAG,"ffmpeg cmd:${cmd_str}")
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
    long_arr.add(viewModel.info.video_duration*1000)//ms as unit
    val str_arr=ArrayList<String>()
    val date= Date(System.currentTimeMillis())
    val formatter= SimpleDateFormat("yyyyMMddHHmmss", activity.resources.configuration.locales[0])
    val task_log_path= activity.filesDir.absolutePath+"/ffmpeg"+formatter.format(date)+".log"
    str_arr.add(target_path)
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