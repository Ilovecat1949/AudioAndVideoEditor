package com.example.audioandvideoeditor.components

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
import com.example.audioandvideoeditor.R

@Composable
fun UserCenterScreen(
    nextDestination:(route:String)->Unit
){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement= Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Spacer(
            modifier = Modifier.height(10.dp)
        )
        TextButton(onClick = {
            nextDestination(VideoFilesList.route)
        }) {
            Text(text= LocalContext.current.resources.getString(R.string.user_audio_and_video_list))
        }
//        Spacer(
//            modifier = Modifier.height(10.dp)
//        )
//        TextButton(onClick = { /*TODO*/ }) {
//            Text(text=LocalContext.current.resources.getString(R.string.user_image_list))
//        }
        Spacer(
            modifier = Modifier.height(10.dp)
        )
        TextButton(onClick = {
            nextDestination(Config.route)
        }) {
            Text(text=LocalContext.current.resources.getString(R.string.settings))
        }
    }
}