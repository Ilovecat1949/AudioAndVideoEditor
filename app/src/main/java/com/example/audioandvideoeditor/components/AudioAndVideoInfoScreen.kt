package com.example.audioandvideoeditor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audioandvideoeditor.MainActivity
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.viewmodel.AudioAndVideoInfoViewModel
import kotlinx.coroutines.delay

@Composable
fun AVInfoScreen(
    activity: MainActivity,
    path:String,
    avInfoViewModel: AudioAndVideoInfoViewModel= viewModel()
){
    val life= rememberLifecycle()
    life.onLifeCreate {
        if(activity.tasks_binder_flag) {
            avInfoViewModel.tasksBinder = activity.tasksBinder
            avInfoViewModel.getInfo(path)
            avInfoViewModel.tasks_binder_flag.value=activity.tasks_binder_flag
        }
        else{
            avInfoViewModel.tasks_binder_flag.value=false
        }
    }
    if(avInfoViewModel.tasks_binder_flag.value) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val info = avInfoViewModel.info
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "${LocalContext.current.getString(R.string.path)}:" + path)
//        Spacer(modifier = Modifier.height(10.dp))
//        Text(text=avInfoViewModel.info_text.value)
            if (avInfoViewModel.info_text.value.length > 0) {
                if (info.duration > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "${LocalContext.current.getString(R.string.duration)}(s):${info.duration}")
                }
                if (info.width > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "${LocalContext.current.getString(R.string.video_resolution)}:${info.width}×${info.height}")
                }
                if (info.frame_rate > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "${LocalContext.current.getString(R.string.frame_rate)}(fps):${info.frame_rate}")
                }
                if (info.video_bit_rate > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "${LocalContext.current.getString(R.string.video_bit_rate)}(bit/s):${info.video_bit_rate}")
                }
                if (info.sample_rate > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "${LocalContext.current.getString(R.string.sample_rate)}(HZ):${info.sample_rate}")
                }
                if (info.channels > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "${LocalContext.current.getString(R.string.channels)}:${info.channels}")
                }
                if (info.audio_bit_rate > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "${LocalContext.current.getString(R.string.audio_bit_rate)}(bit/s):${info.audio_bit_rate}")
                }
            }
        }
    }
    LaunchedEffect(true){
        while (true){
            if(activity.tasks_binder_flag) {
                avInfoViewModel.tasksBinder = activity.tasksBinder
                avInfoViewModel.getInfo(path)
                avInfoViewModel.tasks_binder_flag.value=activity.tasks_binder_flag
                break
            }
            else{
                avInfoViewModel.tasks_binder_flag.value=false
            }
            delay(100)
        }
    }
}