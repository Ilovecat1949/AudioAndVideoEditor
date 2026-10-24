package com.example.audioandvideoeditor.components

import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.entity.AudioInfo
import com.example.audioandvideoeditor.entity.VideoInfo
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.utils.FilesUtils
import com.example.audioandvideoeditor.viewmodel.FilesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

private val TAG = "FileSelectionScreen"

@Composable
fun FileSelectionScreen(
    selectionMode: Int = 0, // 🌟 0: 单选, 1: 多选
    backDestination: () -> Unit,
    onFilesSelected: (List<File>) -> Unit, // 支持回调选中文件列表
    nextDestination:()->Unit,
    filesViewModel: FilesViewModel = viewModel()
){
    filesViewModel.selectionMode = selectionMode
    filesViewModel.setContentResolver(LocalContext.current.contentResolver)

    val life = rememberLifecycle()
    life.onLifeCreate {
        if (filesViewModel.parent == null) {
            val file = File(Environment.getExternalStorageDirectory().path)
            file.listFiles()?.let {
                filesViewModel.filesList.addAll(it)
            }
            filesViewModel.parent = file
        }
        filesViewModel.setSortCriteriaAndOrder(ConfigsUtils.files_sort_flag)
    }

    Scaffold(
        topBar = {
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(color = MaterialTheme.colorScheme.primary)
                    .padding(top = 20.dp)
            ){
                Row (
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.width(160.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_arrow_back_24),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(start = 20.dp)
                            .size(32.dp)
                            .clickable { backDestination() }
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(text = LocalContext.current.resources.getString(R.string.select_file), fontSize = 20.sp, color = Color.White)
                }
                Row (
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_check_24),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(end = 20.dp)
                            .size(32.dp)
                            .clickable {
                                if (filesViewModel.selectedFiles.isNotEmpty()) {
                                    onFilesSelected(filesViewModel.selectedFiles.toList())
                                    filesViewModel.selectedFiles.clear()
                                    filesViewModel.parent = null
                                    filesViewModel.filesList.clear()
                                    nextDestination()
                                }
                            }
                    )
                }
            }
        },
    ) { innerPadding ->
        ShowListScreen(innerPadding, filesViewModel)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShowListScreen(padding: PaddingValues, filesViewModel: FilesViewModel){
    filesViewModel.setContentResolver(LocalContext.current.contentResolver)
    val context = LocalContext.current
    val titles = remember {
        listOf(
            context.getString(R.string.video),
            context.getString(R.string.audio),
            context.getString(R.string.file),
        )
    }
    val pagerState = rememberPagerState(initialPage = 0) { titles.size }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = padding.calculateTopPadding())
    ) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                            filesViewModel.show_flag.value = index
                        }
                    },
                    text = { Text(title) }
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            Column(
                modifier = Modifier.fillMaxWidth()
            ){
                when(page){
                    0 -> VideoFilesList(padding, filesViewModel)
                    1 -> AudioFilesList(padding, filesViewModel)
                    2 -> FilesList(padding, filesViewModel)
                }
                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f))
            }
        }
    }
}

@Composable
private fun FilesList(
    padding: PaddingValues,
    filesViewModel: FilesViewModel
){
    var showSortOptionsDialog by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(filesViewModel.sortCriteria, filesViewModel.sortOrder) {
        if (filesViewModel.displayFilesList.isNotEmpty()) {
            coroutineScope.launch {
                lazyListState.animateScrollToItem(0)
            }
        }
    }
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ){
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.clickable{
                        if (filesViewModel.parent != null && filesViewModel.parent!!.path != Environment.getExternalStorageDirectory().path) {
                            if (filesViewModel.parent!!.parentFile != null) {
                                filesViewModel.parent!!.parentFile
                                    ?.listFiles()
                                    ?.let {
                                        filesViewModel.filesList.clear()
                                        filesViewModel.filesList.addAll(it)
                                        filesViewModel.parent = filesViewModel.parent!!.parentFile
                                    }
                            } else {
                                filesViewModel.filesList.clear()
                                filesViewModel.filesList.add(filesViewModel.parent!!)
                                filesViewModel.parent = null
                            }
                        }
                    },
                ) {
                    Spacer(modifier = Modifier.width(20.dp))
                    Icon(painter = painterResource(id = R.drawable.baseline_folder_24), contentDescription = null)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = "..")
                }
                IconButton(
                    onClick = { showSortOptionsDialog = true },
                    modifier = Modifier.wrapContentSize(Alignment.TopEnd)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.sort_24px),
                        modifier = Modifier.size(48.dp),
                        contentDescription = null
                    )
                }
            }
        }
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = it.calculateTopPadding() + 30.dp)
        ){
            items(count = filesViewModel.displayFilesList.size){
                ShowFile(filesViewModel, it)
            }
        }
    }
    BackHandler(enabled = filesViewModel.backHandler_flag ){
        if (filesViewModel.parent != null && filesViewModel.parent!!.path != Environment.getExternalStorageDirectory().path) {
            if (filesViewModel.parent!!.parentFile != null) {
                filesViewModel.parent!!.parentFile
                    ?.listFiles()
                    ?.let {
                        filesViewModel.filesList.clear()
                        filesViewModel.filesList.addAll(it)
                        filesViewModel.parent = filesViewModel.parent!!.parentFile
                    }
            } else {
                filesViewModel.filesList.clear()
                filesViewModel.filesList.add(filesViewModel.parent!!)
                filesViewModel.parent = null
            }
        }
        filesViewModel.backHandler_flag = (filesViewModel.show_flag.value == 2 && filesViewModel.parent != null && filesViewModel.parent!!.path != Environment.getExternalStorageDirectory().path)
    }
    if (showSortOptionsDialog) {
        var sortCriteria2 by remember {
            mutableStateOf(filesViewModel.sortCriteria)
        }

        var sortOrder2  by remember {
            mutableStateOf(filesViewModel.sortOrder)
        }
        AlertDialog(
            onDismissRequest = { showSortOptionsDialog = false },
            title = { Text(stringResource(id = R.string.sort)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement =  Arrangement.Center,
                ){
                    // Sort Criteria Options
                    Text(
                        text = stringResource(id = R.string.criteria),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement =  Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            RadioButton(
                                selected = sortCriteria2 == 0,
                                onClick = { sortCriteria2=0 }
                            )
                            Text(stringResource(id = R.string.file_name))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = sortCriteria2 == 1,
                                onClick = { sortCriteria2=1 }
                            )
                            Text(stringResource(id = R.string.size))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = sortCriteria2 == 2,
                                onClick = { sortCriteria2=2 }
                            )
                            Text(stringResource(id = R.string.modified_time))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sort Order Toggle
                    Text(
                        text = stringResource(id = R.string.order),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = sortOrder2 == 0,
                            onCheckedChange = { sortOrder2= if (sortOrder2 == 1) 0 else 1 }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(if (sortOrder2 == 1) stringResource(id = R.string.asc) else stringResource(id = R.string.desc))
                    }
                }
            },
            confirmButton = {
                val context= LocalContext.current
                TextButton(onClick = {
                    val flag=sortCriteria2*3+sortOrder2
                    if(flag!=ConfigsUtils.files_sort_flag){
                        ConfigsUtils.setFilesSortFlag(context,flag )
                        filesViewModel.setSortCriteriaAndOrder(flag)
                    }
                    showSortOptionsDialog = false
                }) {
                    Text(stringResource(id = R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun ShowFile(
    filesViewModel: FilesViewModel,
    id: Int
){
    val file = filesViewModel.displayFilesList[id]
    // 🌟 获取当前文件在选中列表中的位置索引 (-1 表示未选中)
    val NOT_SELECTED = -1
    val selectedIndex = filesViewModel.selectedFiles.indexOfFirst { it.path == file.path }
    val isSelected = selectedIndex != NOT_SELECTED

    if(file.isFile){
        Column(
            modifier = Modifier.fillMaxWidth()
        ){
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // 🌟 点击整行即可触发选择切换
                        filesViewModel.toggleFileSelection(file)
                    }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_insert_drive_file_24),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = file.name,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 5,
                    softWrap = true,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.wrapContentWidth()
                ){
                    Spacer(modifier = Modifier.width(5.dp))
                    if (filesViewModel.selectionMode == 0) {
                        // 🌟 单选模式：渲染标准 Checkbox
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { _ ->
                                filesViewModel.toggleFileSelection(file)
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        // 🌟 多选模式：渲染带数字序号的圆形徽标
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Text(
                                    text = (selectedIndex + 1).toString(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
        }
    }
    else if(file.isDirectory){
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val dir = file
                    if (dir.listFiles() != null) {
                        filesViewModel.filesList.clear()
                        filesViewModel.parent = dir
                        filesViewModel.backHandler_flag =
                            (filesViewModel.show_flag.value == 2 && filesViewModel.parent != null && filesViewModel.parent!!.path != Environment.getExternalStorageDirectory().path)
                        dir.listFiles()?.let { it1 ->
                            filesViewModel.filesList.addAll(it1)
                        }
                    }
                }
        ){
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(painter = painterResource(id = R.drawable.baseline_folder_24), contentDescription = null)
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = file.name,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 5,
                    softWrap = true
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
        }
    }
}

@Composable
private fun VideoFilesList(
    padding: PaddingValues,
    filesViewModel: FilesViewModel
){
    val videosPager = filesViewModel.videosPager
    val lazyPagingItems = videosPager.flow.collectAsLazyPagingItems()
    LazyVerticalGrid (
        columns = GridCells.Adaptive(minSize = 128.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }){
            Spacer(modifier = Modifier.fillMaxWidth().height(50.dp))
        }
        if (lazyPagingItems.loadState.refresh == LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Waiting for items to load from the backend",
                    modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
        items(count = lazyPagingItems.itemCount) { index ->
            val item = lazyPagingItems[index]
            if (item != null) {
                ShowVideoFileInfo(filesViewModel, item)
            }
        }
        if (lazyPagingItems.loadState.append == LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }){
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }){
            Spacer(modifier = Modifier.fillMaxWidth().height(50.dp))
        }
    }
}

@Composable
private fun AudioFilesList(
    padding: PaddingValues,
    filesViewModel: FilesViewModel
){
    val audiosPager = filesViewModel.audiosPager
    val lazyPagingItems = audiosPager.flow.collectAsLazyPagingItems()
    LazyVerticalGrid (
        columns = GridCells.Adaptive(minSize = 128.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }){
            Spacer(modifier = Modifier.fillMaxWidth().height(50.dp))
        }
        if (lazyPagingItems.loadState.refresh == LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Waiting for items to load from the backend",
                    modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
        items(count = lazyPagingItems.itemCount) { index ->
            val item = lazyPagingItems[index]
            if (item != null) {
                ShowAudioFileInfo(filesViewModel, item)
            }
        }
        if (lazyPagingItems.loadState.append == LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }){
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }){
            Spacer(modifier = Modifier.fillMaxWidth().height(50.dp))
        }
    }
}

@Composable
private fun ShowVideoFileInfo(
    filesViewModel: FilesViewModel,
    info: VideoInfo
){
    val selectedIndex = filesViewModel.selectedFiles.indexOfFirst { it.path == info.path }
    val isSelected = selectedIndex != -1
    val context = LocalContext.current
    var thumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(info.uri) {
        withContext(Dispatchers.IO){
            filesViewModel.mutex.withLock {
                if(thumbnailBitmap == null){
                    val bitmap = filesViewModel.thumbnailBitmapArray.find { it.first == info.path }?.second
                    if(bitmap != null){
                        thumbnailBitmap = bitmap
                    } else {
                        thumbnailBitmap = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                            FilesUtils.getThumbnail(context.contentResolver, info.uri)
                        } else {
                            FilesUtils.getVideoCover(info.path)
                        }
                        if(thumbnailBitmap != null){
                            if(filesViewModel.thumbnailBitmapArray.size > filesViewModel.thumbnailsMaxNum){
                                val pair = filesViewModel.thumbnailBitmapArray.first()
                                filesViewModel.thumbnailBitmapArray.removeAt(0)
                                pair.second.recycle()
                            }
                            filesViewModel.thumbnailBitmapArray.add(Pair(info.path, thumbnailBitmap!!))
                        }
                    }
                    thumbnailBitmap?.prepareToDraw()
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ){
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .clickable {
                    filesViewModel.toggleFileSelection(File(info.path))
                }
        ){
            if(thumbnailBitmap != null) {
                Image(
                    bitmap = thumbnailBitmap!!.asImageBitmap(),
                    modifier = Modifier
                        .width(128.dp)
                        .height(128.dp)
                        .background(color = Color.Black, shape = RoundedCornerShape(10.dp)),
                    contentDescription = null
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_video_file_24),
                    tint = Color.Yellow,
                    modifier = Modifier
                        .width(128.dp)
                        .height(128.dp)
                        .background(color = Color.Black, shape = RoundedCornerShape(10.dp)),
                    contentDescription = null
                )
            }

            // 🌟 选中状态展示：单选显示勾选图标，多选显示数字序号徽标
            if (isSelected) {
                if (filesViewModel.selectionMode == 0) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_check_24),
                        tint = Color.White,
                        modifier = Modifier
                            .padding(start = 64.dp, top = 64.dp)
                            .size(64.dp),
                        contentDescription = null
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(32.dp)
                            .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (selectedIndex + 1).toString(),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = info.name,
            overflow = TextOverflow.Ellipsis,
            maxLines = 5,
            softWrap = true
        )
        Spacer(modifier = Modifier.height(50.dp))
    }
}

@Composable
private fun ShowAudioFileInfo(
    filesViewModel: FilesViewModel,
    info: AudioInfo
){
    val selectedIndex = filesViewModel.selectedFiles.indexOfFirst { it.path == info.path }
    val isSelected = selectedIndex != -1

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ){
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .clickable {
                    filesViewModel.toggleFileSelection(File(info.path))
                }
        ){
            Icon(
                painter = painterResource(id = R.drawable.baseline_audio_file_24),
                tint = Color.Green,
                modifier = Modifier
                    .width(128.dp)
                    .height(128.dp)
                    .background(color = Color.White, shape = RoundedCornerShape(10.dp)),
                contentDescription = null
            )

            // 🌟 选中状态展示：单选显示勾选图标，多选显示数字序号徽标
            if (isSelected) {
                if (filesViewModel.selectionMode == 0) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_check_24),
                        tint = Color.White,
                        modifier = Modifier
                            .padding(start = 64.dp, top = 64.dp)
                            .size(64.dp),
                        contentDescription = null
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(32.dp)
                            .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (selectedIndex + 1).toString(),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = info.name,
            overflow = TextOverflow.Ellipsis,
            maxLines = 5,
            softWrap = true
        )
        Spacer(modifier = Modifier.height(50.dp))
    }
}