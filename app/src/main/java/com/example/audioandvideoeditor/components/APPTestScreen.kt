package com.example.audioandvideoeditor.components

import android.util.DisplayMetrics
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp


@Composable
fun APPTestScreen(){
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            val a=100/0
        }) {
            Text("测试除以0")
        }
    }
//    LocalContext.current
//    val configuration = LocalConfiguration.current
//    Spacer(modifier = Modifier
//        .width(configuration.screenWidthDp.dp)
//        .height(configuration.screenHeightDp.dp)
//        .background(color = Color.Red)
//    )
//    val displayMetrics: DisplayMetrics = LocalContext.current.resources.displayMetrics
//    val dpWidth = displayMetrics.widthPixels / displayMetrics.density
//    val dpHeight = displayMetrics.heightPixels / displayMetrics.density
//    Spacer(modifier = Modifier
//        .width(dpWidth.dp)
//        .height(dpHeight.dp)
//        .background(color = Color.Red)
//    )
//    Log.d("Screen Dimensions (dp)", "Width: " + dpWidth + "dp, Height: " + dpHeight + "dp")
//    Log.d("configuration Screen Dimensions (dp)", "Width: " + configuration.screenWidthDp + "dp, Height: " + configuration.screenHeightDp + "dp")

}