package com.example.audioandvideoeditor.components

import android.util.Log
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.viewmodel.ContentItem
import com.example.audioandvideoeditor.viewmodel.FileReadingViewModel
import com.example.audioandvideoeditor.viewmodel.FileReadingViewModel_2
import kotlinx.coroutines.launch
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
    Log.d(TAG,"viewModel.visibleContent:${viewModel.visibleContent.size}")
    }
    Column(modifier = Modifier.fillMaxSize()) {
        if (viewModel.isLoading.value) {
           // CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("...")
        } else if (viewModel.errorMessage.value != null) {
            Text(text = viewModel.errorMessage.value!!, color = MaterialTheme.colorScheme .error)
        } else {
//            if(visibleContentState.isNotEmpty()) {
                Log.d(TAG,"scrollState.value:${offset}")
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun FileReadingScreen_2(file: File, readFromEnd: Boolean) {
    val viewModel: FileReadingViewModel_2 = viewModel()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var offset by remember { mutableStateOf(0f) }
    LaunchedEffect(file, readFromEnd) {
        viewModel.openFile(file, readFromEnd)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (viewModel.isLoading.value) {
            //CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("...")
        } else if (viewModel.errorMessage.value != null) {
            Text(text = viewModel.errorMessage.value!!, color = MaterialTheme.colorScheme .error)
        } else {
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    text = viewModel.fileContent.value,
                    modifier = Modifier
                        .fillMaxSize()
//                        .scrollable(scrollState, Orientation.Vertical)
                        .scrollable(
                            orientation = Orientation.Vertical,
                            // Scrollable state: describes how to consume
                            // scrolling delta and update offset
                            state = rememberScrollableState { delta ->
                                offset += delta
                                delta
                            }
                        )
                )
            }
            Slider(
                value = viewModel.readingProgress.value,
                onValueChange = { viewModel.jumpToPosition(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }
    }

    LaunchedEffect(offset) {
        coroutineScope.launch {
            //Log.d(TAG,"scrollState.value:${offset}")
            val visibleHeight = 100 // Adjust this value based on your screen size and text size
            if (offset>0) {
                viewModel.loadMoreContent(false) // Scrolling up
            } else if (offset<0) {
                viewModel.loadMoreContent(true) // Scrolling down
            }
            offset=0f
        }
    }
}




































