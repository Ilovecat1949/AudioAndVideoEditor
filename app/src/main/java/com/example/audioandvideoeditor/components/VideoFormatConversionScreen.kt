package com.example.audioandvideoeditor.components

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.Gravity
import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.example.audioandvideoeditor.MainActivity
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.entity.TaskInfo
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.utils.FormatsUtils
import com.example.audioandvideoeditor.utils.TextsUtils
import com.example.audioandvideoeditor.viewmodel.VideoFormatConversionViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


private val TAG="VideoFormatConversionScreen"
@Composable
fun VideoFormatConversionScreen(
    activity: MainActivity,
    file: File,
    nextDestination:()->Unit,
    viewModel: VideoFormatConversionViewModel = viewModel()
){
    val life=rememberLifecycle()
    life.onLifeCreate {
        viewModel.tasksBinder=activity.tasksBinder
    }
    VideoConversionScreen(
        activity, file, nextDestination, viewModel
    )
}

@Composable
private fun VideoConversionScreen(
    activity: MainActivity,
    file: File,
    nextDestination:()->Unit,
    viewModel: VideoFormatConversionViewModel
){
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

            Spacer(modifier = Modifier.height(15.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ){
                Button(
                    onClick = {
                        viewModel.editFileNameFlag.value=true
                    },
                    modifier = Modifier

                ) {
                    Text(text=context.getString(R.string.export))
                }
                Spacer(modifier = Modifier.width(20.dp))
                Button(
                    onClick = {
                        viewModel.showFileAttributeFlag.value=true
                    },
                    modifier = Modifier
                ) {
                    Text(text= stringResource(id = R.string.attributes))
                }
            }
            Spacer(modifier = Modifier.height(15.dp))
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.changeTargetFormatFlag.value = true
                    }
            ){
                Spacer(modifier = Modifier.height(15.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text= stringResource(id = R.string.target_format)
                        , fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    {
                        Text(viewModel.targetFormatText.value)
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }
                Spacer(modifier = Modifier.height(15.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)
            }
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.changeAudioFormatFlag.value = true
                    }
            ){
                Spacer(modifier = Modifier.height(15.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text= stringResource(id = R.string.audio_format)
                        , fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    {
                        Text(viewModel.audioFormatText.value)
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }
                Spacer(modifier = Modifier.height(15.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)
            }
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.changeVideoFormatFlag.value = true
                    }
            ){
                Spacer(modifier = Modifier.height(15.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text= stringResource(id = R.string.video_encoding_format)
                        , fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    {
                        Text(viewModel.videoFormatText.value)
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }
                Spacer(modifier = Modifier.height(15.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)
            }
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.changeVideoResolutionFlag.value = true
                    }
            ){
                Spacer(modifier = Modifier.height(15.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text= stringResource(id = R.string.video_resolution)
                        , fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    {
                        Text(viewModel.videoResolutionText.value)
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }
                Spacer(modifier = Modifier.height(15.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)
            }
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.changeVideoBitRateFlag.value = true
                    }
            ){
                Spacer(modifier = Modifier.height(15.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("${stringResource(id = R.string.video_bit_rate)}(kb/s)"
                        , fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    {
                        Text(viewModel.videoBitRateText.value)
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }
                Spacer(modifier = Modifier.height(15.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)
            }
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.changeAudioBitRateFlag.value = true
                    }
            ){
                Spacer(modifier = Modifier.height(15.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("${stringResource(id = R.string.audio_bit_rate)}(kb/s)"
                        , fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    {
                        Text(viewModel.audioBitRateText.value)
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }
                Spacer(modifier = Modifier.height(15.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)
            }
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.changeFrameRateFlag.value = true
                    }
            ){
                Spacer(modifier = Modifier.height(15.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("${stringResource(id = R.string.frame_rate)}(fps)"
                        , fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    {
                        Text(viewModel.frameRateText.value)
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }
                Spacer(modifier = Modifier.height(15.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)
            }
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.changeSampleRateFlag.value = true
                    }
            ){
                Spacer(modifier = Modifier.height(15.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("${stringResource(id = R.string.sample_rate)}(HZ)"
                        , fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    {
                        Text(viewModel.sampleRateText.value)
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }
                Spacer(modifier = Modifier.height(15.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)
            }
        }
        selectTargetFormat(viewModel)
        selectAudioFormat(viewModel)
        selectVideoFormat(viewModel)
        selectVideoResolution2(viewModel)
        editVideoBitRate(viewModel)
        editAudioBitRate(viewModel)
        editFrameRate(viewModel)
        selectSampleRate(viewModel)
        showFileAttribute(file, viewModel)
        editFileName(
            activity, file, nextDestination, viewModel
        )
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

private fun startConversion(
    activity: MainActivity,
    file: File,
    nextDestination:()->Unit,
    viewModel: VideoFormatConversionViewModel
){
    var cmd_str="ffmpeg -i input_file "//ffmpeg -i ${file.path}
    var audio_cmd_str=""
    val targetFormatSupport=FormatsUtils.FullySupportsFormatsConfigs[viewModel.targetFormatOptions[viewModel.checkTargetFormatFlag.value]]

    if(
        viewModel.checkAudioFormatFlag.value==0
        && viewModel.checkAudioBitRateFlag.value==0
        && viewModel.checkSampleRateFlag.value==0
        && targetFormatSupport!!.audioCodecs.contains(viewModel.source_audio_format)
     )
    {
        audio_cmd_str=audio_cmd_str+"-c:a copy "
    }
    else{
        if(viewModel.checkAudioFormatFlag.value>0){
            when(viewModel.audioFormatOptions[viewModel.checkAudioFormatFlag.value]){
                "AAC"->{audio_cmd_str=audio_cmd_str+"-acodec aac " }
                "FLAC"->{audio_cmd_str=audio_cmd_str+"-acodec flac -strict -2 "}
                "VORBIS"->{audio_cmd_str=audio_cmd_str+"-acodec vorbis "}
                "OPUS"->{audio_cmd_str=audio_cmd_str+"-acodec opus -strict -2 "}
                "AC3"->{audio_cmd_str=audio_cmd_str+"-acodec ac3 "}
            }
        }
        if(viewModel.checkAudioBitRateFlag.value>0){
            audio_cmd_str=audio_cmd_str+"-b:a ${viewModel.audio_bit_rate} "
        }
        if(viewModel.checkSampleRateFlag.value>0){
            audio_cmd_str=audio_cmd_str+"-ar ${viewModel.sample_rate} "
        }
    }
    cmd_str=cmd_str+audio_cmd_str
    var video_cmd_str=""
    if(
        viewModel.checkVideoFormatFlag.value==0
        && viewModel.checkVideoResolutionFlag.value==0
        && viewModel.checkVideoBitRateFlag.value==0
        && viewModel.checkFrameRateFlag.value==0
        && targetFormatSupport!!.videoCodecs.contains(viewModel.source_video_format)
    ){
        video_cmd_str=video_cmd_str+"-c:v copy "
    }
    else{
        if(viewModel.checkVideoFormatFlag.value>0){
            when(viewModel.videoFormatOptions[viewModel.checkVideoFormatFlag.value]){
                "H.264(AVC)"->{video_cmd_str=video_cmd_str+"-vcodec libx264 -preset ultrafast -q:v 5 "}
                "MPEG-1"->{video_cmd_str=video_cmd_str+"-vcodec mpeg1video  -q:v 5 "}
                "MPEG-2"->{video_cmd_str=video_cmd_str+"-vcodec mpeg2video  -q:v 5 "}
                "MPEG-4 Part 2"->{video_cmd_str=video_cmd_str+"-vcodec mpeg4  -q:v 5 "}
            }
        }
        else if(targetFormatSupport!!.videoCodecs.contains("H.264(AVC)")){
            video_cmd_str=video_cmd_str+"-vcodec libx264 -preset ultrafast -q:v 5 "
        }
        //${viewModel.videoResolutionOptions[viewModel.checkVideoResolutionFlag.value]}
        if(viewModel.checkVideoResolutionFlag.value>0){
            video_cmd_str += "-vf scale=${viewModel.videoResolutionOptions[viewModel.checkVideoResolutionFlag.value]} "
        }
        if(viewModel.checkVideoBitRateFlag.value>0){
            video_cmd_str=video_cmd_str+"-b:v ${viewModel.video_bit_rate} "
        }
        if(viewModel.checkFrameRateFlag.value>0){
            video_cmd_str=video_cmd_str+"-r ${viewModel.frame_rate} "
        }
    }
    cmd_str=cmd_str+video_cmd_str
    val target_path="${ConfigsUtils.target_dir}/${viewModel.target_name}.${viewModel.targetFormatText.value.lowercase()}"
    cmd_str += "output_file "
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
    long_arr.add(viewModel.info.video_duration*1000)//ms as unit
    val str_arr=ArrayList<String>()
    val date= Date(System.currentTimeMillis())
    val formatter= SimpleDateFormat("yyyyMMddHHmmss", activity.resources.configuration.locales[0])
    val task_log_path= activity.filesDir.absolutePath+"/ffmpeg"+formatter.format(date)+".log"
    str_arr.add(target_path)
    str_arr.add(task_log_path)
    //ffmpeg -i ${file.path}
//    str_arr.add("ffmpeg")
//    str_arr.add("-i")
//    str_arr.add(file.path)
    str_arr.addAll(command_arg_list)
//    str_arr.add(target_path)
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
private fun editAudioBitRate(
    viewModel: VideoFormatConversionViewModel
){
    var text by remember { mutableStateOf("") }
    if(viewModel.changeAudioBitRateFlag.value){
        AlertDialog(
            onDismissRequest = { viewModel.changeAudioBitRateFlag.value = false },
            title = { Text("${stringResource(id = R.string.audio_bit_rate)}(kb/s)") },
            text = {
                TextField(
                    value = text,
                    onValueChange = {
                        if(it.isDigitsOnly()){
                            text = it
                        }

                    },
                    label = { Text(stringResource(id = R.string.audio_bit_rate)) }
                )
            },
            confirmButton = {
                Button(onClick = {
                        viewModel.audio_bit_rate=text.toLong()*1000
                        viewModel.audioBitRateText.value=text
                        viewModel.checkAudioBitRateFlag.value=1
                    viewModel.changeAudioBitRateFlag.value = false
                }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                Button(onClick = { viewModel.changeAudioBitRateFlag.value  = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}


@Composable
private fun showFileAttributeItem(
    attribute:String,
    value:String
){
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.width(10.dp))
        Text(attribute  ,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
        )
        {
            Text(value)
            Spacer(modifier = Modifier.width(10.dp))
        }
    }
}
@Composable
private fun showFileAttribute(
    file: File,
    viewModel: VideoFormatConversionViewModel
){
    if(viewModel.showFileAttributeFlag.value){
        AlertDialog(
            onDismissRequest = { viewModel.showFileAttributeFlag.value = false },
            title = { Text(stringResource(id = R.string.attributes)) },
            text = {
              LazyColumn(
              ){
                  item {
                      Column(
                          modifier = Modifier
                              .fillMaxWidth()
                      ){
                        showFileAttributeItem(stringResource(id = R.string.file_name),file.name)
                        Spacer(modifier = Modifier.height(20.dp))
                        showFileAttributeItem(stringResource(id = R.string.path),file.path)
                        Spacer(modifier = Modifier.height(20.dp))
                        showFileAttributeItem(stringResource(id = R.string.size),TextsUtils.getSizeText(file.length()))
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        Spacer(modifier = Modifier.height(20.dp))
                        showFileAttributeItem(stringResource(id = R.string.date),sdf.format(file.lastModified()))
                        Spacer(modifier = Modifier.height(20.dp))
                        showFileAttributeItem(stringResource(id = R.string.extension),file.extension)
                        Spacer(modifier = Modifier.height(20.dp))
                      }
                  }
                  if(viewModel.info.video_bit_rate>0L) {
                      item {
                          showFileAttributeItem(stringResource(id = R.string.video_duration),TextsUtils.millisecondsToString(viewModel.info.video_duration*1000))
                          Spacer(modifier = Modifier.height(20.dp))
                          showFileAttributeItem(
                              stringResource(R.string.video_resolution),
                              "${viewModel.info.width}x${viewModel.info.height}"
                          )
                          Spacer(modifier = Modifier.height(20.dp))
                          showFileAttributeItem(
                              "${stringResource(R.string.video_bit_rate)}(kb/s)",
                              "${viewModel.info.video_bit_rate / 1000}"
                          )
                          Spacer(modifier = Modifier.height(20.dp))
                          showFileAttributeItem("${stringResource(R.string.frame_rate)}(fps)", "${viewModel.info.frame_rate}")
                          Spacer(modifier = Modifier.height(20.dp))
                          showFileAttributeItem(stringResource(R.string.video_encoding_format), viewModel.info.video_codec_type)
                          Spacer(modifier = Modifier.height(20.dp))
                      }
                  }
                  if(viewModel.info.audio_bit_rate>0) {
                      item {
                          showFileAttributeItem(stringResource(R.string.audio_format), viewModel.info.audio_codec_type)
                          Spacer(modifier = Modifier.height(20.dp))
                          showFileAttributeItem(
                              "${stringResource(R.string.audio_bit_rate)}(kb/s)",
                              "${viewModel.info.audio_bit_rate / 1000}"
                          )
                          Spacer(modifier = Modifier.height(20.dp))
                          showFileAttributeItem("${stringResource(R.string.sample_rate)}(fps)", "${viewModel.info.sample_rate}")
                          Spacer(modifier = Modifier.height(20.dp))
                      }
                  }
              }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.showFileAttributeFlag.value = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
//                Button(onClick = { viewModel.showFileAttributeFlag.value  = false }) {
//                    Text(stringResource(R.string.cancel))
//                }
            }
        )
    }
}


@Composable
private fun selectTargetFormat(
    viewModel: VideoFormatConversionViewModel
){
if(viewModel.changeTargetFormatFlag.value){
    var select_text by remember{
        mutableStateOf(viewModel.targetFormatText.value)
    }
    val context= LocalContext.current
    AlertDialog(
        onDismissRequest = { viewModel.changeTargetFormatFlag.value = false },
        title = { Text(stringResource(id = R.string.target_format)) },
        text = {
            //viewModel.targetFormatText.value=viewModel.targetFormatOptions[0]
            Column(modifier=Modifier.selectableGroup()) {
                viewModel.targetFormatOptions.forEach {
                  text ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (text == select_text),
                                onClick = {
                                    select_text = text
                                },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (text == select_text),
                            onClick = null // null recommended for accessibility with screen readers
                        )
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }

            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.targetFormatText.value = select_text
                    viewModel.checkTargetFormatFlag.value =
                        viewModel.targetFormatOptions.indexOf(select_text)
                    if (viewModel.checkAudioFormatFlag.value != 0 &&
                        !FormatsUtils
                            .FullySupportsFormatsConfigs[viewModel.targetFormatText.value]!!
                            .audioCodecs.contains(select_text)
                    ) {
                        viewModel.audioFormatText.value =
                            "${viewModel.source_audio_format}(${context.getString(R.string.original_format)})"
                        viewModel.checkAudioFormatFlag.value = 0
                    }
                    if (viewModel.checkVideoFormatFlag.value != 0 &&
                        !FormatsUtils
                            .FullySupportsFormatsConfigs[viewModel.targetFormatText.value]!!
                            .videoCodecs.contains(select_text)
                    ) {
                        viewModel.audioFormatText.value =
                            "${viewModel.source_video_format}(${context.getString(R.string.original_format)})"
                        viewModel.checkVideoFormatFlag.value = 0
                    }
                    viewModel.changeTargetFormatFlag.value = false
            }) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            Button(onClick = { viewModel.changeTargetFormatFlag.value  = false }) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
}


@Composable
private fun selectAudioFormat(
    viewModel: VideoFormatConversionViewModel
){
    var select_index by remember {
        mutableStateOf(viewModel.checkAudioFormatFlag.value)
    }
    val context= LocalContext.current
    if(viewModel.changeAudioFormatFlag.value){
        AlertDialog(
            onDismissRequest = { viewModel.changeAudioFormatFlag.value = false },
            title = { Text(stringResource(id = R.string.audio_format)) },
            text = {
                Column(modifier=Modifier.selectableGroup()) {
                    viewModel.audioFormatOptions.forEach {
                            text ->
                        if(text.isEmpty() || FormatsUtils
                                .FullySupportsFormatsConfigs[viewModel.targetFormatText.value]!!
                                .audioCodecs.contains(text)
                            ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .selectable(
                                        selected = (select_index == viewModel.audioFormatOptions.indexOf(
                                            text
                                        )),
                                        onClick = {
                                            select_index = viewModel.audioFormatOptions.indexOf(
                                                text
                                            )

                                        },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (select_index == viewModel.audioFormatOptions.indexOf(
                                        text
                                    )),
                                    onClick = null // null recommended for accessibility with screen readers
                                )
                                Text(
                                    text = if (text.isEmpty()) stringResource(id = R.string.original_format) else text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (select_index!=0) {
                        viewModel.audioFormatText.value = viewModel.audioFormatOptions[select_index]
                    } else {
                        viewModel.audioFormatText.value =
                            "${viewModel.source_audio_format}(${context.getString(R.string.original_format)})"
                    }
                    viewModel.checkAudioFormatFlag.value =select_index
                    viewModel.changeAudioFormatFlag.value = false
                }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                Button(onClick = { viewModel.changeAudioFormatFlag.value  = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun selectVideoFormat(
    viewModel: VideoFormatConversionViewModel
){
    var select_index by remember {
        mutableStateOf(viewModel.checkVideoFormatFlag.value)
    }
    val context= LocalContext.current
    if(viewModel.changeVideoFormatFlag.value){
        AlertDialog(
            onDismissRequest = { viewModel.changeVideoFormatFlag.value = false },
            title = { Text(stringResource(id = R.string.video_encoding_format)) },
            text = {
                Column(modifier=Modifier.selectableGroup()) {
                    viewModel.videoFormatOptions.forEach {
                        text ->
                        if(text.isEmpty() ||
                                FormatsUtils
                                    .FullySupportsFormatsConfigs[viewModel.targetFormatText.value]!!
                                    .videoCodecs.contains(text))
                        {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .selectable(
                                        selected = (select_index == viewModel.videoFormatOptions.indexOf(
                                            text
                                        )),
                                        onClick = {
                                            select_index = viewModel.videoFormatOptions.indexOf(
                                                text
                                            )
                                        },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (select_index == viewModel.videoFormatOptions.indexOf(
                                        text
                                    )),
                                    onClick = null // null recommended for accessibility with screen readers
                                )
                                Text(
                                    text = if (text.isEmpty()) stringResource(id = R.string.original_format) else text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (select_index!=0) {
                        viewModel.videoFormatText.value = viewModel.videoFormatOptions[select_index]
                    } else {
                        viewModel.videoFormatText.value =
                            "${viewModel.source_video_format}(${context.getString(R.string.original_format)})"
                    }
                    viewModel.checkVideoFormatFlag.value =select_index
                    viewModel.changeVideoFormatFlag.value = false
                }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                Button(onClick = { viewModel.changeVideoFormatFlag.value  = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun selectVideoResolution(
    viewModel: VideoFormatConversionViewModel
){
    if(viewModel.changeVideoResolutionFlag.value){
        AlertDialog(
            onDismissRequest = { viewModel.changeVideoResolutionFlag.value = false },
            title = { Text("选择视频分辨率") },
            text = {
                Column(modifier=Modifier.selectableGroup()) {
                    viewModel.videoResolutionOptions.forEach {
                            text ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (viewModel.checkVideoResolutionFlag.value == viewModel.videoResolutionOptions.indexOf(
                                        text
                                    )),
                                    onClick = {
                                        if (text != viewModel.videoResolutionOptions[0] && text != "${viewModel.source_width}×${viewModel.source_height}") {
                                            viewModel.videoResolutionText.value = text
                                            val parts = text.split("x")
                                            viewModel.video_width = parts[0].toInt()
                                            viewModel.video_height = parts[1].toInt()
                                        } else {
                                            viewModel.videoResolutionText.value =
                                                "${viewModel.source_width}×${viewModel.source_height}(${viewModel.videoResolutionOptions[0]})"
                                            viewModel.video_height = viewModel.source_height
                                            viewModel.video_width = viewModel.source_width
                                        }
                                        viewModel.checkVideoResolutionFlag.value =
                                            viewModel.videoResolutionOptions.indexOf(text)
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (viewModel.checkVideoResolutionFlag.value ==  viewModel.videoResolutionOptions.indexOf(text)),
                                onClick = null // null recommended for accessibility with screen readers
                            )
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }

                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.changeVideoResolutionFlag.value = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                Button(onClick = { viewModel.changeVideoResolutionFlag.value  = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun selectVideoResolution2(
    viewModel: VideoFormatConversionViewModel
){
    var select_index by remember {
        mutableStateOf(viewModel.checkVideoResolutionFlag.value)
    }
    var fillflag by remember {
        mutableStateOf(viewModel.checkVideoResolutionFlag.value==-1 )
    }
    var height by remember {
        mutableStateOf("${viewModel.video_height}")
    }
    var width by remember {
        mutableStateOf("${viewModel.video_width}")
    }
    val context= LocalContext.current
    if(viewModel.changeVideoResolutionFlag.value){
        AlertDialog(
            onDismissRequest = { viewModel.changeVideoResolutionFlag.value = false },
            title = { Text(stringResource(id = R.string.video_resolution)) },
            text = {
                LazyColumn(modifier= Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                ){
                    item{
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = fillflag,
                                    onClick = {
                                        fillflag = !fillflag
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected =fillflag  ,
                                onClick = null // null recommended for accessibility with screen readers
                            )
                            Spacer(modifier =Modifier.width(20.dp))
                            TextField(
                                modifier = Modifier.weight(1f),
                                value =width,
                                onValueChange = {
                                    if(it.isDigitsOnly()){
                                        width=it
                                    }
                                },
                                label = {
                                    Text(stringResource(id = R.string.width))
                                }
                            )
                            Spacer(modifier =Modifier.width(10.dp))
                            TextField(
                                modifier = Modifier.weight(1f),
                                value =height,
                                onValueChange = {
                                    if(it.isDigitsOnly()){
                                        height=it
                                    }
                                },
                                label = {
                                    Text(stringResource(id = R.string.height))
                                }
                            )
                        }
                    }
                    items(viewModel.videoResolutionOptions){
                          text ->
                      Row(
                          Modifier
                              .fillMaxWidth()
                              .height(56.dp)
                              .selectable(
                                  selected = (select_index == viewModel.videoResolutionOptions.indexOf(
                                      text
                                  )) && !fillflag,
                                  onClick = {
                                      select_index = viewModel.videoResolutionOptions.indexOf(
                                          text
                                      )
                                      fillflag = false

                                  },
                                  role = Role.RadioButton
                              )
                              .padding(horizontal = 16.dp),
                          verticalAlignment = Alignment.CenterVertically
                      ) {
                          RadioButton(
                              selected = (select_index==  viewModel.videoResolutionOptions.indexOf(text))&&!fillflag,
                              onClick = null // null recommended for accessibility with screen readers
                          )
                          Text(
                              text = if(text.isEmpty()) stringResource(id = R.string.original_resolution) else text,
                              style = MaterialTheme.typography.bodyLarge,
                              modifier = Modifier.padding(start = 16.dp)
                          )
                      }
                  }
                }
            },
            confirmButton = {
                val ctx=LocalContext.current
                Button(onClick = {
                    if(fillflag){
                        if(width.length>0 && height.length>0){
                            viewModel.videoResolutionText.value =
                                "${width}×${height}"
                            viewModel.video_height = width.toInt()
                            viewModel.video_width = height.toInt()
                            viewModel.checkVideoResolutionFlag.value=-1
                            viewModel.changeVideoResolutionFlag.value = false
                        }
                        else{
                            sendToast(ctx,ctx.getString(R.string.incorrect_or_invalid_input))
                        }
                    }
                    else{
                        if (select_index!=0) {
                            viewModel.videoResolutionText.value = viewModel.videoResolutionOptions[select_index]
                            val parts = viewModel.videoResolutionOptions[select_index].split("x")
                            viewModel.video_width = parts[0].toInt()
                            viewModel.video_height = parts[1].toInt()
                        } else {
                            viewModel.videoResolutionText.value =
                                "${viewModel.source_width}×${viewModel.source_height}(${ctx.getString(R.string.original_resolution)})"
                            viewModel.video_height = viewModel.source_height
                            viewModel.video_width = viewModel.source_width
                        }
                        viewModel.checkVideoResolutionFlag.value =select_index
                        viewModel.changeVideoResolutionFlag.value = false
                    }
                }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                Button(onClick = { viewModel.changeVideoResolutionFlag.value  = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private  fun sendToast(ctx: Context, text:String){
    val toast = Toast.makeText( ctx, text, Toast.LENGTH_SHORT)
    toast.setGravity(Gravity.CENTER, 0, 0)
    toast.show()
}

@Composable
private fun editVideoBitRate(
    viewModel: VideoFormatConversionViewModel
){
    var text by remember { mutableStateOf("") }
    val ctx= LocalContext.current
    if(viewModel.changeVideoBitRateFlag.value){
        AlertDialog(
            onDismissRequest = { viewModel.changeVideoBitRateFlag.value = false },
            title = { Text("${stringResource(id = R.string.video_bit_rate)}(kb/s)") },
            text = {
                TextField(
                    value = text,
                    onValueChange = {
                        if(it.isDigitsOnly()){
                            text = it
                        }

                    },
                    label = { Text(stringResource(id = R.string.video_bit_rate)) }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if(text.isNotEmpty()){
                        viewModel.video_bit_rate=text.toLong()*1000
                        viewModel.videoBitRateText.value=text
                        viewModel.checkVideoBitRateFlag.value=1
                        viewModel.changeVideoBitRateFlag.value = false
                    }
                    else{
                        sendToast(ctx,ctx.getString(R.string.incorrect_or_invalid_input))
                    }

                }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                Button(onClick = { viewModel.changeVideoBitRateFlag.value  = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}




@Composable
private fun editFrameRate(
    viewModel: VideoFormatConversionViewModel
){

    if(viewModel.changeFrameRateFlag.value){
        var text by remember { mutableStateOf("") }
        val ctx= LocalContext.current
        AlertDialog(
            onDismissRequest = { viewModel.changeFrameRateFlag.value = false },
            title = { Text("${stringResource(id = R.string.frame_rate)}(fps)") },
            text = {
                TextField(
                    value = text,
                    onValueChange = {
                        if(it.isDigitsOnly()){
                            text = it
                        }

                    },
                    label = { Text(stringResource(id = R.string.frame_rate)) }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if(text.isNotEmpty()){
                        viewModel.frame_rate=text.toInt()
                        viewModel.frameRateText.value=text
                        viewModel.checkFrameRateFlag.value=1
                        viewModel.changeFrameRateFlag.value = false
                    }
                    else{
                        sendToast(ctx,ctx.getString(R.string.incorrect_or_invalid_input))
                    }

                }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                Button(onClick = { viewModel.changeFrameRateFlag.value  = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun selectSampleRate(
    viewModel: VideoFormatConversionViewModel
){
    if(viewModel.changeSampleRateFlag.value){
        var select_index by remember {
            mutableStateOf(viewModel.checkSampleRateFlag.value)
        }
        val context= LocalContext.current
        AlertDialog(
            onDismissRequest = { viewModel.changeSampleRateFlag.value = false },
            title = { Text("${stringResource(id = R.string.sample_rate)}(HZ)") },
            text = {
                Column(modifier=Modifier.selectableGroup()) {
                    viewModel.sampleRateOptions.forEach {
                            text ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (select_index == viewModel.sampleRateOptions.indexOf(
                                        text
                                    )),
                                    onClick = {
                                        select_index = viewModel.sampleRateOptions.indexOf(text)
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (select_index ==  viewModel.sampleRateOptions.indexOf(text)),
                                onClick = null // null recommended for accessibility with screen readers
                            )
                            Text(
                                text = if(text.isEmpty()) stringResource(id = R.string.original_sample_rate) else text,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }

                }
            },
            confirmButton = {
                Button(onClick = {
                    if (select_index!=0) {
                        viewModel.sampleRateText.value = viewModel.sampleRateOptions[select_index]
                        viewModel.sample_rate = viewModel.sampleRateText.value.toInt()
                    } else {
                        viewModel.sampleRateText.value =
                            "${viewModel.source_sample_rate}(${context.getString(R.string.original_sample_rate)})"
                        viewModel.sample_rate = viewModel.source_sample_rate
                    }
                    viewModel.checkSampleRateFlag.value =select_index
                    viewModel.changeSampleRateFlag.value = false
                }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                Button(onClick = { viewModel.changeSampleRateFlag.value  = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun editFileName(
    activity: MainActivity,
    file: File,
    nextDestination:()->Unit,
    viewModel: VideoFormatConversionViewModel
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
                    startConversion(activity, file, nextDestination, viewModel)
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

