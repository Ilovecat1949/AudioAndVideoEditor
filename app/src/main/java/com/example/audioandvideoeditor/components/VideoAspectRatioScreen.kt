package com.example.audioandvideoeditor.components

import android.content.Context
import android.net.Uri
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.audioandvideoeditor.MainActivity
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.entity.TaskInfo
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.utils.FormatsUtils
import com.example.audioandvideoeditor.viewmodel.VideoAspectRatioViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private  val TAG="VideoAspectRatioScreen"
@Composable
fun VideoAspectRatioScreen(
    activity: MainActivity,
    file: File,
    nextDestination:()->Unit,
    viewModel: VideoAspectRatioViewModel= viewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    viewModel.videoScreenWidth = configuration.screenWidthDp
    viewModel.videoScreenHeight = configuration.screenHeightDp*3/5
    val displayMetrics: DisplayMetrics = LocalContext.current.resources.displayMetrics
    val dpWidth = displayMetrics.widthPixels / displayMetrics.density
    val dpHeight = displayMetrics.heightPixels / displayMetrics.density
    viewModel.AspectRatio[1]=Pair(dpWidth.toInt(),dpHeight.toInt())

    LaunchedEffect(Unit) {
        viewModel.initSource(file.path,activity)
    }
    viewModel.mediaItem.value =  MediaItem.fromUri(Uri.parse(file.path))
    if(viewModel.mediaItem.value==null){
        Log.d(TAG,"viewModel.mediaItem.value==null")
    }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var newDimensions by remember {
       mutableStateOf(Pair(1,1))
    }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            viewModel.mediaItem.value?.let { setMediaItem(it) }
            prepare()
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        isLoading = false
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    isLoading = false
                    loadError = "Video loading failed: ${error.message}"
                }
            })
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Preview Area
        Box(
            modifier = Modifier
                .width(viewModel.videoScreenWidth.dp)
                .height(viewModel.videoScreenHeight.dp)
//                .background(color = Color.Black)
            ,
            contentAlignment = Alignment.Center
        ) {
            if (isLoading||!viewModel.dimensionsReady.value ) {
               // CircularProgressIndicator()
               Text("...")
            } else if (loadError != null) {
                Text(text = loadError!!, color = Color.Red)
            } else {
                viewModel.mediaItem.value?.let { _ ->
                    newDimensions = viewModel.getNewVideoDimensions()
                    AndroidView(
                        factory = {
                            PlayerView(context).apply {
                                this.player = player
                                player.prepare()
                                player.play()
                            }
                        },
                        modifier = Modifier
                            .size(newDimensions.first.dp, newDimensions.second.dp)
                            .background(color = viewModel.selectedBackgroundColor.value)
                    )
                }
            }
        }

        // Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Aspect Ratio Selection
            Text(text= stringResource(id = R.string.select_aspect_ratio), fontSize = 18.sp)
            LazyRow(
                verticalAlignment=Alignment.CenterVertically
            ) {
                items(
                    count = viewModel.AspectRatio.size
                ){
                    val ratio=viewModel.AspectRatio[it]
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.selectedAspectIndex==it) Color.Red
                            else MaterialTheme.colorScheme.primary
                        ) ,
                        onClick = {
                            if(
                                it!=2
                            ){
                                viewModel.setAspectRatio(ratio)
                                viewModel.selectedAspectIndex=it
                                newDimensions = viewModel.getNewVideoDimensions()
                            }
                            else{
                               viewModel.editAspectRatioFlag=true
                            }
                    }) {
                        when(it){
                           0->Text(stringResource(id = R.string.original_ratio))
                           1->Text(stringResource(id = R.string.screen_ratio))
                           2->Text(stringResource(id = R.string.custom_ratio))
                           else ->Text("${ratio.first}:${ratio.second}")
                        }
                    }
//                    Column(
//                        modifier = Modifier
//                            .size(500.dp)
//                            .background(color = Color.Black,shape= RoundedCornerShape(10.dp))
//                    ){
//                    }
                    Spacer(modifier = Modifier.width(20.dp))
                }
//                VideoAspectRatioViewModel.AspectRatio.entries.forEach { ratio ->
//                    Button(onClick = {
//                        viewModel.setAspectRatio(ratio)
//                        newDimensions = viewModel.getNewVideoDimensions()
//                    }) {
//                        Text("${ratio.ratio.first}:${ratio.ratio.second}")
//                    }
//                    Spacer(modifier = Modifier.width(8.dp))
//                }
            }

            // Background Color Selection
            Text(stringResource(id = R.string.select_background_color), fontSize = 18.sp)
            Row(modifier = Modifier.fillMaxWidth()) {
                //Color.Black, Color.Red, Color.Magenta, Color.White
                viewModel.colorList.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(color)
                            .clickable { viewModel.setBackgroundColor(color) }
                            .border(
                                width = 2.dp,
                                color = if (color == viewModel.selectedBackgroundColor.value) MaterialTheme.colorScheme.primary
                                else color
                            )
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                }
            }

            // Export Button (Placeholder)
            Button(onClick = {
                viewModel.editFileNameFlag.value=true
            },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.export))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }
    editFileName(activity, file, nextDestination, viewModel)
    editAspectRatio(viewModel)
}


@Composable
private fun editFileName(
    activity: MainActivity,
    file: File,
    nextDestination:()->Unit,
    viewModel:VideoAspectRatioViewModel
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
                    startExportVideo(activity, file, nextDestination, viewModel)
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
    activity: MainActivity,
    file: File,
    nextDestination:()->Unit,
    viewModel: VideoAspectRatioViewModel
){
    var cmd_str="ffmpeg -i input_file "
    if(
        FormatsUtils.FullySupportsFormatsConfigs.containsKey(file.extension.uppercase())
        && FormatsUtils.FullySupportsFormatsConfigs[file.extension.uppercase()]!!.videoCodecs.contains("H.264(AVC)")){
        cmd_str+=" -vcodec libx264 -preset ultrafast -q:v 5"
    }
    val target_path="${ConfigsUtils.target_dir}/${viewModel.target_name}.${file.extension}"
    val new_resolution=viewModel.getNewResolution(viewModel.info.width,viewModel.info.height,viewModel.selectedAspectRatio.value.first,viewModel.selectedAspectRatio.value.second)
    cmd_str+=" -vf pad=${new_resolution.first}:${new_resolution.second}:x=(${new_resolution.first}-iw)/2:y=(${new_resolution.second}-ih)/2:color=${viewModel.colorToString(viewModel.selectedBackgroundColor.value)}"
    cmd_str+=" output_file"
    Log.d(TAG,"ffmpeg cmd:${cmd_str}")
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
    long_arr.add(viewModel.info.video_duration*1000)
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
    nextDestination()
}



private  fun sendToast(ctx: Context, text:String){
    val toast = Toast.makeText( ctx, text, Toast.LENGTH_SHORT)
    toast.setGravity(Gravity.CENTER, 0, 0)
    toast.show()
}
@Composable
private fun editAspectRatio(
    viewModel: VideoAspectRatioViewModel
){
    if(viewModel.editAspectRatioFlag){
        val context= LocalContext.current
        var width by remember { mutableStateOf(viewModel.AspectRatio[2].first) }
        var height by remember { mutableStateOf(viewModel.AspectRatio[2].second) }
        AlertDialog(
            onDismissRequest = { viewModel.editAspectRatioFlag = false },
            title = { Text(stringResource(id = R.string.ratio)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextField(
                        value = width.toString(),
                        onValueChange = {
                            if(it.isDigitsOnly()){
                                width = it.toInt()
                            }

                        },
                        label = { Text(stringResource(id = R.string.width)) }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    TextField(
                        value = height.toString(),
                        onValueChange = {
                            if(it.isDigitsOnly()){
                                height = it.toInt()
                            }
                        },
                        label = { Text(stringResource(id = R.string.height)) }
                    )
                }

            },
            confirmButton = {
                Button(onClick = {
                    if(height>0 && width>0){
                        viewModel.selectedAspectIndex=2
                        viewModel.AspectRatio[2]= Pair(width,height)
                        viewModel.selectedAspectRatio.value=viewModel.AspectRatio[2]
                        viewModel.editAspectRatioFlag  = false
                    }
                    else{
                        sendToast(context,context.getString(R.string.incorrect_or_invalid_input))
                    }

                }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                Button(onClick = { viewModel.editAspectRatioFlag  = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}


