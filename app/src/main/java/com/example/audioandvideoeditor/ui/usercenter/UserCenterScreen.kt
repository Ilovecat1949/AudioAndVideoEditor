package com.example.audioandvideoeditor.ui.usercenter

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
import com.example.audioandvideoeditor.navigation.Destination

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
            nextDestination(Destination.VideoFilesList.route)
        }) {
            Text(text= LocalContext.current.resources.getString(R.string.user_audio_and_video_list))
        }
        Spacer(
            modifier = Modifier.height(10.dp)
        )
        TextButton(onClick = {
            nextDestination(Destination.FilesList.route)
        }) {
            Text(text="文件列表")
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
            nextDestination(Destination.Config.route)
        }) {
            Text(text=LocalContext.current.resources.getString(R.string.settings))
        }
    }
}