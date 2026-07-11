package com.example.audioandvideoeditor.ui.functionscenter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.navigation.Destination

@Composable
fun FunctionsCenterScreen(
    nextDestination:(route:String)->Unit,
    setNextToNextDestination:(route:String)->Unit,
){
    FunctionsListScreen2(nextDestination,setNextToNextDestination)
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
                    .background(color = Color(0xFFFFDBD1), shape=RoundedCornerShape(10.dp))
                    .clickable {
                        nextDestination(Destination.FFmpegCommands.route)
                    }
            ){
                Text(
                    text= LocalContext.current.getString(R.string.ffmpeg_command_line),
                    modifier = Modifier
                        .padding(10.dp)
                )
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
                        setNextToNextDestination(Destination.VideoCompress.route)
                        nextDestination(Destination.FileSelection.route)
                    }
            ){
                Text(
                    text= LocalContext.current.getString(R.string.video_compress),
                    modifier = Modifier
                        .padding(10.dp)
                )
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
                        nextDestination(Destination.Recording.route)
                    }
            ){
                Text(text=stringResource(R.string.record))
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
                        setNextToNextDestination(Destination.VideoFormatConversion.route)
                        nextDestination(Destination.FileSelection.route)
                    }
            ){
                Text(
                    text= LocalContext.current.getString(R.string.video_format_conversion),
                    modifier = Modifier
                        .padding(10.dp)
                )
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
                        setNextToNextDestination(Destination.VideoSegmenter.route)
                        nextDestination(Destination.FileSelection.route)
                    }
            ){
                Text(
                    text= LocalContext.current.getString(R.string.video_duration_trimming)
                    , modifier = Modifier.padding(10.dp)
                )
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
                        setNextToNextDestination(Destination.VideoCrop.route)
                        nextDestination(Destination.FileSelection.route)
                    }
            ){
                Text(
                    text= LocalContext.current.getString(R.string.video_screen_cropping)
                    , modifier = Modifier.padding(10.dp)
                )
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
                        setNextToNextDestination(Destination.VideoAspectRatio.route)
                        nextDestination(Destination.FileSelection.route)
                    }
            ){
                Text(
                    text= LocalContext.current.getString(R.string.video_scaling),
                    modifier = Modifier.padding(10.dp)

                )
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
                        setNextToNextDestination(Destination.SpeedChange.route)
                        nextDestination(Destination.FileSelection.route)
                    }
            ){
                Text(text= LocalContext.current.getString(R.string.video_shifting),
                    modifier = Modifier.padding(10.dp)
                )
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
                        setNextToNextDestination(Destination.ExtractAudio.route)
                        nextDestination(Destination.FileSelection.route)
                    }
            ){
                Text(
                    text= LocalContext.current.getString(R.string.extract_audio),
                    modifier = Modifier.padding(10.dp)
                )
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
                        setNextToNextDestination(Destination.VideoMute.route)
                        nextDestination(Destination.FileSelection.route)
                    }
            ){
                Text(text= LocalContext.current.getString(R.string.video_muting),
                    modifier = Modifier.padding(10.dp))
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
                        setNextToNextDestination(Destination.AudioAndVideoInfo.route)
                        nextDestination(Destination.FileSelection.route)
                    }
            ){
                Text(
                    text=LocalContext.current.resources.getString(R.string.av_info),
                    modifier = Modifier.padding(10.dp)
                )

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
                        nextDestination(Destination.FFmpegInfo.route)
                    }
            ){
                Text(text=LocalContext.current.resources.getString(R.string.get_ffmpeg_information))
            }
        }
//        item {
//            Column(
//                verticalArrangement = Arrangement.Center,
//                horizontalAlignment = Alignment.CenterHorizontally,
//                modifier = Modifier.height(150.dp)
//                    .width(200.dp)
//                    .background(color = Color(0xFFFFDBD1), shape=RoundedCornerShape(10.dp))
//                    .clickable {
//                        nextDestination(APPTest.route)
//                    }
//            ){
//                Text(text=LocalContext.current.resources.getString(R.string.test))
//            }
//        }
//        item {
//        Column(
//           verticalArrangement = Arrangement.Center,
//            horizontalAlignment = Alignment.CenterHorizontally,
//            modifier = Modifier.height(150.dp)
//                .width(200.dp)
//                .background(color =  Color(0xFFFFDBD1), shape=RoundedCornerShape(10.dp))
//                .clickable {
//                    setNextToNextDestination(ReEncoding.route)
//                    nextDestination(FileSelection.route)
//                }
//        ){
//            Text(text= LocalContext.current.resources.getString(R.string.reencoding))
//        }
//     }
//        item {
//            Column(
//                verticalArrangement = Arrangement.Center,
//                horizontalAlignment = Alignment.CenterHorizontally,
//                modifier = Modifier.height(150.dp)
//                    .width(200.dp)
//                    .background(color = Color(0xFFFFDBD1), shape=RoundedCornerShape(10.dp))
//                    .clickable {
//                        setNextToNextDestination(RePackaging.route)
//                        nextDestination(FileSelection.route)
//                    }
//            ){
//                Text(text= LocalContext.current.getString(R.string.repack))
//            }
//        }
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