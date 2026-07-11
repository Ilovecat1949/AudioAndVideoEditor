package com.example.audioandvideoeditor.components

import android.util.Log
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.viewmodel.ContentItem
import com.example.audioandvideoeditor.viewmodel.FileReadingViewModel
import java.io.File




private val TAG="FileReadingScreen"
@Composable
fun FileReadingScreen(
    file: File,
    readFromEnd: Boolean,
    viewModel: FileReadingViewModel = viewModel()
){
    val life= rememberLifecycle()
    life.onLifeCreate {
        viewModel.openFile(file, readFromEnd)
    }
    FileReadingScreen2(file,readFromEnd,viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileReadingScreen2(file: File, readFromEnd: Boolean,viewModel: FileReadingViewModel ) {
    val visibleContentState = remember { mutableStateListOf<ContentItem>() } // Use mutableStateListOf
    var offset by remember { mutableStateOf(0f) }
    LaunchedEffect(viewModel.itemIdFlag) {
//        snapshotFlow { viewModel.visibleContent }.collectLatest { newList ->
//            visibleContentState.clear()
//            visibleContentState.addAll(newList)
//        }
    visibleContentState.clear()
    visibleContentState.addAll(viewModel.visibleContent)
//    Log.d(TAG,"viewModel.visibleContent:${viewModel.visibleContent.size}")
    }
    Column(modifier = Modifier.fillMaxSize()) {
        if (viewModel.isLoading.value) {
           // CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("...")
        } else if (viewModel.errorMessage.value != null) {
            Text(text = viewModel.errorMessage.value!!, color = MaterialTheme.colorScheme .error)
        } else {
//            if(visibleContentState.isNotEmpty()) {
//                Log.d(TAG,"scrollState.value:${offset}")
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .scrollable(
                            orientation = Orientation.Vertical,
                            // Scrollable state: describes how to consume
                            // scrolling delta and update offset
                            state = rememberScrollableState { delta ->
                                offset += delta
                                delta
                            }
                        )

                ) {
                    //visibleContentState
                    items(visibleContentState, key = { it.id }) { item ->
                        SelectionContainer {
                            Text(text = item.text)
                        }
                        if (item.id == visibleContentState.last().id && offset<0) {
                                offset=0f
                                viewModel.loadMoreContent(true)
                        } else if (item.id == visibleContentState.first().id&& offset>0) {
                            offset=0f
                            viewModel.loadMoreContent(false)
                        }
                    }
//                }
            }
            Slider(
                value = viewModel.readingProgress.value,
                onValueChange = { viewModel.jumpToPosition(it) },
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = remember { MutableInteractionSource() },
                        thumbSize = DpSize(24.dp, 24.dp),
                        // 💡 修复方案 A：不写参数名，直接把主题色塞给第一个参数 (thumbColor)
                        colors = SliderDefaults.colors(MaterialTheme.colorScheme.primary)
                    )
                },
                // 💥 核心：覆盖默认轨道，强制关闭镂空缝隙！
                track = { sliderState ->
                    SliderDefaults.Track(
                        colors = SliderDefaults.colors().copy(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                        ),
                        sliderState = sliderState,
                        thumbTrackGapSize = 0.dp // 👈 就是这个罪魁祸首！把它设为 0.dp，缝隙瞬间闭合。
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }
    }
}




































