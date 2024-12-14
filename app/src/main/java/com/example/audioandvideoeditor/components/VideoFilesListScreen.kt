package com.example.audioandvideoeditor.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.entity.AudioInfo
import com.example.audioandvideoeditor.entity.VideoInfo
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.utils.FilesUtils
import com.example.audioandvideoeditor.viewmodel.VideoFilesListViewModel
import java.io.File

@Composable
fun VideoFilesListScreen(
    videoPlay:(file: File, route:String)->Unit,
    videoFilesListViewModel:VideoFilesListViewModel= viewModel()
){
    videoFilesListViewModel.setContentResolver(LocalContext.current.contentResolver)
    val life= rememberLifecycle()
    life.onLifeCreate {
        videoFilesListViewModel.videoPlay=videoPlay
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
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically)
                {
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(text = LocalContext.current.getString(R.string.video))
                    Spacer(modifier = Modifier.width(5.dp))
                    Checkbox(
                        checked =videoFilesListViewModel.show_flag.value,
                        onCheckedChange = {
                            videoFilesListViewModel.show_flag.value=it
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically)
                {
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(text = LocalContext.current.getString(R.string.audio))
                    Spacer(modifier = Modifier.width(5.dp))
                    Checkbox(
                        checked =!videoFilesListViewModel.show_flag.value,
                        onCheckedChange = {
                            videoFilesListViewModel.show_flag.value=!it
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                }
            }
        }
    ){
            innerPadding->ShowFilesList(innerPadding,videoFilesListViewModel)
    }
}
@Composable
private fun ShowFilesList(padding: PaddingValues,videoFilesListViewModel:VideoFilesListViewModel){
  if(videoFilesListViewModel.show_flag.value){
      VideoFilesList(padding,videoFilesListViewModel)
  }
  else{
      AudioFilesList(padding,videoFilesListViewModel)
  }
}
@Composable
private fun VideoFilesList(
    padding: PaddingValues,
    videoFilesListViewModel:VideoFilesListViewModel
){

    val videosPager=videoFilesListViewModel.videosPager
    val lazyPagingItems = videosPager.flow.collectAsLazyPagingItems()
    LazyVerticalGrid (
        columns = GridCells.Adaptive(minSize = 128.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier
            .padding(top = padding.calculateTopPadding())
    ) {
        item(span = { GridItemSpan(maxLineSpan) }){
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
//                  .background(color = Color.Yellow)
            )
        }
        if (lazyPagingItems.loadState.refresh == LoadState.Loading) {
            item(
                span = { GridItemSpan(maxLineSpan) }
            ) {
                Text(
                    text = "Waiting for items to load from the backend",
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
        items(count = lazyPagingItems.itemCount) { index ->
            val item = lazyPagingItems[index]
//            Text("Index=$index: $item", fontSize = 20.sp)
            if (item != null) {
                ShowVideoFileInfo(item,videoFilesListViewModel)
            }
        }
        if (lazyPagingItems.loadState.append == LoadState.Loading) {
            item(
                span = { GridItemSpan(maxLineSpan) }
            ){
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }){
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
//                  .background(color = Color.Yellow)
            )
        }
    }
}
@Composable
private fun AudioFilesList(
    padding: PaddingValues,
    videoFilesListViewModel:VideoFilesListViewModel
){
    val audiosPager=videoFilesListViewModel.audiosPager
    val lazyPagingItems = audiosPager.flow.collectAsLazyPagingItems()
    LazyVerticalGrid (
        columns = GridCells.Adaptive(minSize = 128.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier
                   .padding(top = padding.calculateTopPadding())
    ) {
        item(span = { GridItemSpan(maxLineSpan) }){
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
//                  .background(color = Color.Yellow)
            )
        }
        if (lazyPagingItems.loadState.refresh == LoadState.Loading) {
            item(
                span = { GridItemSpan(maxLineSpan) }
            ) {
                Text(
                    text = "Waiting for items to load from the backend",
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
        items(count = lazyPagingItems.itemCount) { index ->
            val item = lazyPagingItems[index]
//            Text("Index=$index: $item", fontSize = 20.sp)
            if (item != null) {
                ShowAudioFileInfo(item,videoFilesListViewModel)
            }
        }
        if (lazyPagingItems.loadState.append == LoadState.Loading) {
            item(
                span = { GridItemSpan(maxLineSpan) }
            ){
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }){
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
//                  .background(color = Color.Yellow)
            )
        }
    }
}
@Composable
private fun ShowVideoFileInfo(
    info: VideoInfo,
    videoFilesListViewModel:VideoFilesListViewModel
){
    val  bitmap= FilesUtils.getThumbnail(LocalContext.current.contentResolver,info.uri)
    bitmap?.prepareToDraw()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement= Arrangement.Center,
    ){
        Spacer(modifier = Modifier.height(5.dp))
        if(bitmap!=null) {
            Image(
                bitmap = bitmap,
                modifier = Modifier
                    .width(128.dp)
                    .height(128.dp)
                    .background(color = Color.Black)
                    .clickable {
                        val file=File(info.path)
                        videoFilesListViewModel.videoPlay(file,VideoPlay.route)
                    }
                    ,
                contentDescription = null
            )
        }
        else{
            Icon(painter = painterResource(id = R.drawable.baseline_video_file_24),
                tint = Color.Yellow,
                modifier = Modifier
                    .width(128.dp)
                    .height(128.dp)
                    .background(color = Color.Black)
                ,
                contentDescription = null)
        }
        Spacer(modifier = Modifier.height(5.dp))
        val file_name=info.name
        if(file_name.length<25) {
            Text(text = file_name)
        }
        else{
            Text(text =file_name.substring(0,19)+"..."+file_name.substring(file_name.length-5))
        }
    }
}
@Composable
private fun ShowAudioFileInfo(
    info:AudioInfo,
    videoFilesListViewModel:VideoFilesListViewModel
){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement= Arrangement.Center,
    ){
        Spacer(modifier = Modifier.height(5.dp))
        Icon(painter = painterResource(id = R.drawable.baseline_audio_file_24),
            tint = Color.Green,
            modifier = Modifier
                .width(128.dp)
                .height(128.dp)
                .background(color = Color.White)
                .clickable {
                    val file=File(info.path)
                    videoFilesListViewModel.videoPlay(file,VideoPlay.route)
                }
            ,
            contentDescription = null)
        Spacer(modifier = Modifier.height(5.dp))
        val file_name=info.name
        if(file_name.length<25) {
            Text(text = file_name)
        }
        else{
            Text(text =file_name.substring(0,19)+"..."+file_name.substring(file_name.length-5))
        }
    }
}