package com.example.audioandvideoeditor.components

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.audioandvideoeditor.R

@Composable
fun FunctionsCenterScreen(
    nextDestination:(route:String)->Unit,
    setNextToNextDestination:(route:String)->Unit,
){
    FunctionsListScreen2(nextDestination,setNextToNextDestination)
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
@Composable
private fun FunctionsListScreen2(
    nextDestination:(route:String)->Unit,
    setNextToNextDestination:(route:String)->Unit,
){
    LazyVerticalGrid (
        columns = GridCells.Adaptive(minSize = 128.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .padding(top = 20.dp)
    ){
     item {
        Column(
           verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.height(150.dp)
                .width(200.dp)
                .background(color =  Color(0xFFFFDBD1), shape=RoundedCornerShape(10.dp))
                .clickable {
                    setNextToNextDestination(ReEncoding.route)
                    nextDestination(FileSelection.route)
                }
        ){
            Text(text= LocalContext.current.resources.getString(R.string.reencoding))
        }
     }
        item {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.height(150.dp)
                    .width(200.dp)
                    .background(color = Color(0xFFFFDBD1), shape=RoundedCornerShape(10.dp))
                    .clickable {
                        setNextToNextDestination(AudioAndVideoInfo.route)
                        nextDestination(FileSelection.route)
                    }
            ){
                Text(text=LocalContext.current.resources.getString(R.string.obtain_audio_and_video_information))
            }
        }
        item {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.height(150.dp)
                    .width(200.dp)
                    .background(color = Color(0xFFFFDBD1), shape=RoundedCornerShape(10.dp))
                    .clickable {
                        nextDestination(FFmpegInfo.route)
                    }
            ){
                Text(text=LocalContext.current.resources.getString(R.string.get_ffmpeg_information))
            }
        }
        item {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.height(150.dp)
                    .width(200.dp)
                    .background(color = Color(0xFFFFDBD1), shape=RoundedCornerShape(10.dp))
                    .clickable {
                        setNextToNextDestination(RePackaging.route)
                        nextDestination(FileSelection.route)
                    }
            ){
                Text(text= LocalContext.current.getString(R.string.reencoding))

            }
        }
        item {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.height(150.dp)
                    .width(200.dp)
                    .background(color = Color(0xFFFFDBD1), shape=RoundedCornerShape(10.dp))
                    .clickable {
                        nextDestination(FFmpegCommands.route)
                    }
            ){
                Text(text= LocalContext.current.getString(R.string.ffmpeg_command_line))
            }
        }
        item {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.height(150.dp)
                    .width(200.dp)
                    .background(color = Color(0xFFFFDBD1), shape=RoundedCornerShape(10.dp))
                    .clickable {
                        setNextToNextDestination(VideoClipping.route)
                        nextDestination(FileSelection.route)
                    }
            ){
                Text(text= LocalContext.current.getString(R.string.video_clipping))
            }
        }
    }
}

//Column(
//horizontalAlignment = Alignment.CenterHorizontally,
//verticalArrangement= Arrangement.Center,
//modifier = Modifier
//.fillMaxWidth()
//) {
////        TextButton(onClick = {
////
////        }) {
////            Text(text="FFmpeg信息")
////        }
//    Spacer(
//        modifier = Modifier.height(10.dp)
//    )
//    TextButton(onClick = {
//        setNextToNextDestination(ReEncoding.route)
//        nextDestination(FileSelection.route)
//    }) {
//        Text(text= LocalContext.current.resources.getString(R.string.reencoding))
//    }
////        Spacer(
////            modifier = Modifier.height(10.dp)
////        )
////        TextButton(onClick = {
////
////        }) {
////            Text(text="测试")
////        }
//    Spacer(
//        modifier = Modifier.height(10.dp)
//    )
//    TextButton(onClick = {
//        setNextToNextDestination(AudioAndVideoInfo.route)
//        nextDestination(FileSelection.route)
//    }) {
//        Text(text=LocalContext.current.resources.getString(R.string.obtain_audio_and_video_information))
//    }
//    Spacer(
//        modifier = Modifier.height(10.dp)
//    )
//    TextButton(onClick = {
//        nextDestination(FFmpegInfo.route)
//    }) {
//        Text(text=LocalContext.current.resources.getString(R.string.get_ffmpeg_information))
//    }
//    Spacer(
//        modifier = Modifier.height(10.dp)
//    )
//    TextButton(onClick = {
//        setNextToNextDestination(RePackaging.route)
//        nextDestination(FileSelection.route)
//    }) {
//        Text(text= "重封装")
//    }
//    Spacer(
//        modifier = Modifier.height(10.dp)
//    )
//    TextButton(onClick = {
//        nextDestination(FFmpegCommands.route)
//    }) {
//        Text(text= "FFmpeg命令行")
//    }
//}