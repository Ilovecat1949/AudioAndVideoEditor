package com.example.audioandvideoeditor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audioandvideoeditor.MainActivity
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.viewmodel.FFmpegInfoViewModel

@Composable
fun FFmpegInfoScreen(
    activity: MainActivity,
    ffmpegInfoViewModel: FFmpegInfoViewModel= viewModel()
){
    val life= rememberLifecycle()
    life.onLifeCreate {
        ffmpegInfoViewModel.tasksBinder=activity.tasksBinder
    }
    Scaffold(
        topBar ={
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(
                        color = Color.White
                    )
            ) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = LocalContext.current.resources.getString(R.string.encoder_and_decoder_information))
                Checkbox(
                    checked =(ffmpegInfoViewModel.show_flag.value==0),
                    onCheckedChange = {
                        if(it){
                            ffmpegInfoViewModel.show_flag.value=0
                        }
                    },
                    modifier = Modifier.size(24.dp)
                )
            }}
    ){
     paddingValues ->   FFmpegInfoList(padding = paddingValues,ffmpegInfoViewModel)
    }
}

@Composable
private fun FFmpegInfoList(
    padding: PaddingValues,
    ffmpegInfoViewModel: FFmpegInfoViewModel
){
    LazyColumn(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = padding.calculateTopPadding() + 40.dp)
    ){
        item {
            Text(text=ffmpegInfoViewModel.tasksBinder.getFFmpegInfo(ffmpegInfoViewModel.show_flag.value))
        }
    }
}

