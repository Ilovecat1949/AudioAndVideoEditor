package com.example.audioandvideoeditor.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audioandvideoeditor.utils.AdContent
import com.example.audioandvideoeditor.utils.FilesUtils
import com.example.audioandvideoeditor.utils.FirebaseUtils


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
        Spacer(modifier = Modifier.height(30.dp))
        Button(onClick = {
            FirebaseUtils.reFreshRemoteConfig()
        }) {
            Text("刷新FireBase")
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
//    AdDialog(
//        ads=FirebaseUtils.adUiState,
//        onDismissRequest={}
//    )
}


@Composable
private fun AdItem(ad: AdContent,  adIndex: Int) {
    val content= LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                FilesUtils.openWebLink(content, ad.clickUrl)
            },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 广告图片
//            Image(
//                painter = rememberAsyncImagePainter(ad.imageUrl),
//                contentDescription = ad.title,
//                modifier = Modifier
//                    .size(96.dp)
//                    .clip(RoundedCornerShape(8.dp)),
//                contentScale = ContentScale.Crop
//            )
            NetworkImage(
                imageUrl=ad.imageUrl,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                // 广告标题
                Text(
                    text = ad.title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                // 广告描述
                Text(
                    text = ad.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun AdDialog(
    ads: List<AdContent>,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false) // 允许自定义宽度
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.9f) // 弹窗宽度占比
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "精彩广告推荐",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn {
                    items(ads.size) { it ->
                        AdItem(ad = ads[it], it)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("关闭")
                }
            }
        }
    }
}