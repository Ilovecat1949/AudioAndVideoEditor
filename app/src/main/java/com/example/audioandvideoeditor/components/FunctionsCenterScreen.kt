package com.example.audioandvideoeditor.components

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.audioandvideoeditor.R

@Composable
fun FunctionsCenterScreen(
    nextDestination:(route:String)->Unit,
    setNextToNextDestination:(route:String)->Unit,
){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement= Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
    ) {
//        TextButton(onClick = {
//
//        }) {
//            Text(text="FFmpeg信息")
//        }
        Spacer(
            modifier = Modifier.height(10.dp)
        )
        TextButton(onClick = {
            setNextToNextDestination(ReEncoding.route)
            nextDestination(FileSelection.route)
        }) {
            Text(text= LocalContext.current.resources.getString(R.string.reencoding))
        }
//        Spacer(
//            modifier = Modifier.height(10.dp)
//        )
//        TextButton(onClick = {
//
//        }) {
//            Text(text="测试")
//        }
        Spacer(
            modifier = Modifier.height(10.dp)
        )
        TextButton(onClick = {
            setNextToNextDestination(AudioAndVideoInfo.route)
            nextDestination(FileSelection.route)
        }) {
            Text(text=LocalContext.current.resources.getString(R.string.obtain_audio_and_video_information))
        }
        Spacer(
            modifier = Modifier.height(10.dp)
        )
        TextButton(onClick = {
            nextDestination(FFmpegInfo.route)
        }) {
            Text(text=LocalContext.current.resources.getString(R.string.get_ffmpeg_information))
        }
        Spacer(
            modifier = Modifier.height(10.dp)
        )
        TextButton(onClick = {
            setNextToNextDestination(RePackaging.route)
            nextDestination(FileSelection.route)
        }) {
            Text(text= "重封装")
        }
        Spacer(
            modifier = Modifier.height(10.dp)
        )
        TextButton(onClick = {
            nextDestination(FFmpegCommands.route)
        }) {
            Text(text= "FFmpeg命令行")
        }
    }
}

@Composable
private fun FunctionsListScreen(){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement= Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        TextButton(onClick = {

        }) {
            Text(text="FFmpeg信息")
        }
        Spacer(
            modifier = Modifier.height(10.dp)
        )
        TextButton(onClick = {

        }) {
            Text(text="重编码")
        }
        Spacer(
            modifier = Modifier.height(10.dp)
        )
        TextButton(onClick = {

        }) {
            Text(text="测试")
        }
    }
}