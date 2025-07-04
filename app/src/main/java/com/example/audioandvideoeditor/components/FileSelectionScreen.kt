package com.example.audioandvideoeditor.components

import android.graphics.Bitmap
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
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
import com.example.audioandvideoeditor.utils.FilesUtils
import com.example.audioandvideoeditor.viewmodel.FilesViewModel
import com.example.audioandvideoeditor.viewmodel.VideoFilesListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File


private val TAG="FileSelectionScreen"

@Composable
fun FileSelectionScreen(
    backDestination:()->Unit,
    setFile:(file:File)->Unit,
    nextDestination:()->Unit,
    filesViewModel: FilesViewModel= viewModel()
){
    filesViewModel.setContentResolver(LocalContext.current.contentResolver)
    val life= rememberLifecycle()
    life.onLifeCreate {
        if(filesViewModel.parent==null){
            val file= File(Environment.getExternalStorageDirectory().path)
            file.listFiles()?.let {
                filesViewModel.filesList.addAll(it)
                //filesViewModel.initFilesState()
            }
            filesViewModel.parent= file
        }

    }
    Scaffold(
        topBar = {
            Row (
//                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(color = MaterialTheme.colorScheme.primary)
                    .padding(top = 20.dp)
            ){
                Row (
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier
                        .width(160.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.baseline_arrow_back_24),
                        contentDescription = null,
                        tint =  Color.White ,
                        modifier = Modifier
                            .padding(start = 20.dp)
                            .size(32.dp)
                            .clickable {
                                backDestination()
                            }
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(text = LocalContext.current.resources.getString(R.string.select_file), fontSize =20.sp, color = Color.White )
                }
                Row (
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(painter = painterResource(id = R.drawable.baseline_check_24),
                        contentDescription = null,
                        tint =  Color.White ,
                        modifier = Modifier
                            .padding(end = 20.dp)
                            .size(32.dp)
                            .clickable {
                                if (filesViewModel.file != null) {
                                    setFile(filesViewModel.file!!)
                                    filesViewModel.file = null
                                    filesViewModel.parent = null
                                    filesViewModel.filesList.clear()
                                    filesViewModel.filesState.clear()
                                    nextDestination()
                                }
                            }
                    )
                }
            }
        },

        ) {
            innerPadding ->ShowListScreen(innerPadding,filesViewModel)
        //FilesList(innerPadding,filesViewModel)

    }
}


@Composable
private fun ShowListScreen_20250606(padding: PaddingValues,filesViewModel: FilesViewModel){
    filesViewModel.setContentResolver(LocalContext.current.contentResolver)
    Scaffold(
        topBar ={
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = padding.calculateTopPadding())
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
                    Text(text = LocalContext.current.resources.getString(R.string.video))
                    Spacer(modifier = Modifier.width(5.dp))
                    Checkbox(
                        checked =(filesViewModel.show_flag.value==0),
                        onCheckedChange = {
                            if(it){
                                filesViewModel.show_flag.value=0
                            }
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
                    Text(text = LocalContext.current.resources.getString(R.string.audio))
                    Spacer(modifier = Modifier.width(5.dp))
                    Checkbox(
                        checked =(filesViewModel.show_flag.value==1),
                        onCheckedChange = {
                            if(it){
                                filesViewModel.show_flag.value=1
                            }
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
                    Text(text = LocalContext.current.resources.getString(R.string.file))
                    Spacer(modifier = Modifier.width(5.dp))
                    Checkbox(
                        checked =(filesViewModel.show_flag.value==2),
                        onCheckedChange = {
                            if(it){
                                filesViewModel.show_flag.value=2
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                }
            }
        }
    ){
            innerPadding->
        ShowList(innerPadding, filesViewModel )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShowListScreen(padding: PaddingValues,filesViewModel: FilesViewModel){
    filesViewModel.setContentResolver(LocalContext.current.contentResolver)
    val context= LocalContext.current
    val titles = remember {
        listOf(
            context.getString(R.string.video),
            context.getString(R.string.audio),
            context.getString(R.string.file),
        )
    }
    val pagerState = rememberPagerState(initialPage = 0) {
        titles.size
    }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = padding.calculateTopPadding())

    ) {
        // 顶部 TabRow
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primary, // 标签背景色
            contentColor = MaterialTheme.colorScheme.onPrimary // 标签内容颜色（文字和指示器）
        ) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                            filesViewModel.show_flag.value=index
                        }
                    },
                    text = { Text(title) }
                )
            }
        }
        // 页面内容
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
            //.weight(1f) // 让内容占据剩余空间
        ) { page ->
//            val padding= PaddingValues(5.dp)
            Column(
                modifier = Modifier.fillMaxWidth()
            ){
                when(page){
                    0->VideoFilesList(padding,filesViewModel)
                    1->AudioFilesList(padding, filesViewModel)
                    2->FilesList(padding ,filesViewModel)
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
    LazyColumn(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
//            .padding(top = padding.calculateTopPadding() + 40.dp)
    ){
        item {
            Row(
            modifier = Modifier
//                .padding(top = padding.calculateTopPadding(), start = 20.dp)
                .height(40.dp)
                .fillMaxWidth()
                .clickable {
                    if (filesViewModel.parent != null && filesViewModel.parent!!.path != Environment.getExternalStorageDirectory().path) {
                        if (filesViewModel.parent!!.parentFile != null) {
                            filesViewModel.parent!!.parentFile
                                ?.listFiles()
                                ?.let {
                                    filesViewModel.filesList.clear()
                                    filesViewModel.filesList.addAll(it)
                                    //filesViewModel.initFilesState()
                                    filesViewModel.parent = filesViewModel.parent!!.parentFile
                                }
                        } else {
                            filesViewModel.filesList.clear()
                            filesViewModel.filesList.add(filesViewModel.parent!!)
                            //filesViewModel.initFilesState()
                            filesViewModel.parent = null
                        }

                    }
                }
        ){
            Icon(painter = painterResource(id = R.drawable.baseline_folder_24), contentDescription = null)
            Spacer(modifier = Modifier.width(20.dp))
            Text(text="..")
        } }
        items(count =filesViewModel.filesList.size){
            ShowFile2(filesViewModel,it)
        }
    }
    BackHandler(enabled =filesViewModel.backHandler_flag ){
        if (filesViewModel.parent != null && filesViewModel.parent!!.path != Environment.getExternalStorageDirectory().path) {
            if (filesViewModel.parent!!.parentFile != null) {
                filesViewModel.parent!!.parentFile
                    ?.listFiles()
                    ?.let {
                        filesViewModel.filesList.clear()
                        filesViewModel.filesList.addAll(it)
                        //filesViewModel.initFilesState()
                        filesViewModel.parent = filesViewModel.parent!!.parentFile
                    }
            } else {
                filesViewModel.filesList.clear()
                filesViewModel.filesList.add(filesViewModel.parent!!)
                //filesViewModel.initFilesState()
                filesViewModel.parent = null
            }

        }
        filesViewModel.backHandler_flag=(filesViewModel.show_flag.value==2 &&filesViewModel.parent != null && filesViewModel.parent!!.path != Environment.getExternalStorageDirectory().path )
    }
}

@Composable
private fun ShowFile(
    filesViewModel: FilesViewModel,
    id:Int
){
    val file=filesViewModel.filesList[id]
    if(!filesViewModel.filesState.containsKey(file.path)){
        filesViewModel.filesState[file.path]= mutableStateOf(false)
    }
    if(file.isFile){
        Row(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_insert_drive_file_24),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(40.dp))
//            if(file.name.length<20) {
//                Text(text = file.name)
//            }
//            else{
//                Text(text =file.name.substring(0,14)+"..."+file.name.substring(file.name.length-5))
//            }
            if(file.name.length<=19) {
                Text(text = file.name)
            }
            else{
                Text(text =file.name.substring(0,11)+"..."+file.name.substring(file.name.length-5))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ){
                    Checkbox(
                        checked =filesViewModel.filesState[file.path]!!.value,
                        onCheckedChange = {
                            filesViewModel.updateFilesState(file.path,it)
                            if(it){
                                filesViewModel.file=file
                            }
                            else{
                                filesViewModel.file=null
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    )
            }
        }
    }
    else if(file.isDirectory){
        Row(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth()
                .clickable {
                    val dir = file
                    if (dir.listFiles() != null) {
                        filesViewModel.filesList.clear()
                        filesViewModel.parent = dir
                        dir
                            .listFiles()
                            ?.let { it1 ->
                                filesViewModel.filesList.addAll(it1)
                                filesViewModel.initFilesState()
                            }
                    }
                }
        ){
            Icon(painter = painterResource(id = R.drawable.baseline_folder_24), contentDescription = null)
            Spacer(modifier = Modifier.width(40.dp))
            if(file.name.length<=19) {
                Text(text = file.name)
            }
            else{
                Text(text =file.name.substring(0,11)+"..."+file.name.substring(file.name.length-5))
            }
        }
    }
}

@Composable
private fun ShowFile2(
    filesViewModel: FilesViewModel,
    id:Int
){
    val file=filesViewModel.filesList[id]
    if(!filesViewModel.filesState.containsKey(file.path)){
        filesViewModel.filesState[file.path]= mutableStateOf(false)
    }
    if(file.isFile){
        Column (
            modifier = Modifier
                .fillMaxWidth()

        ){
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_insert_drive_file_24),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text =file.name,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 5,
                    softWrap = true
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ){
                    Spacer(modifier = Modifier.width(5.dp))
                    Checkbox(
                        checked =filesViewModel.filesState[file.path]!!.value,
                        onCheckedChange = {
                            filesViewModel.updateFilesState(file.path,it)
                            if(it){
                                filesViewModel.file=file
                            }
                            else{
                                filesViewModel.file=null
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
        }
    }
    else if(file.isDirectory){
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val dir = file
                    if (dir.listFiles() != null) {
                        filesViewModel.filesList.clear()
                        filesViewModel.parent = dir
                        filesViewModel.backHandler_flag =
                            (filesViewModel.show_flag.value == 2 && filesViewModel.parent != null && filesViewModel.parent!!.path != Environment.getExternalStorageDirectory().path)
                        dir
                            .listFiles()
                            ?.let { it1 ->
                                filesViewModel.filesList.addAll(it1)
                                filesViewModel.initFilesState()
                            }
                    }
                }
        ){
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Icon(painter = painterResource(id = R.drawable.baseline_folder_24), contentDescription = null)
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text =file.name,
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
private fun ShowList(padding: PaddingValues,filesViewModel: FilesViewModel){
    if(filesViewModel.show_flag.value==0){
        VideoFilesList(padding,filesViewModel)
    }
    else if(filesViewModel.show_flag.value==1){
        AudioFilesList(padding ,filesViewModel)
    }
    else if(filesViewModel.show_flag.value==2){
        FilesList(padding , filesViewModel)
    }
}


@Composable
private fun VideoFilesList(
    padding: PaddingValues,
    filesViewModel: FilesViewModel
){

    val videosPager=filesViewModel.videosPager
    val lazyPagingItems = videosPager.flow.collectAsLazyPagingItems()
    LazyVerticalGrid (
        columns = GridCells.Adaptive(minSize = 128.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier
//                  .padding(top = padding.calculateTopPadding())
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
                ShowVideoFileInfo(filesViewModel,item)
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
    filesViewModel: FilesViewModel
){
    val audiosPager=filesViewModel.audiosPager
    val lazyPagingItems = audiosPager.flow.collectAsLazyPagingItems()
    LazyVerticalGrid (
        columns = GridCells.Adaptive(minSize = 128.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier
//            .padding(top = padding.calculateTopPadding())
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
                ShowAudioFileInfo(filesViewModel,item)
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
    filesViewModel: FilesViewModel,
    info: VideoInfo
){
    if(!filesViewModel.filesState.containsKey(info.path)){
         filesViewModel.filesState[info.path]= mutableStateOf(false)
     }
    val context = LocalContext.current
    var thumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(info.uri) {
        withContext(Dispatchers.IO){
            filesViewModel.mutex.withLock {
                if(thumbnailBitmap==null){
                    val bitmap=filesViewModel.thumbnailBitmapArray.find { it.first==info.path }?.second
                    if(bitmap!=null){
                        thumbnailBitmap=bitmap
                    }
                    else{
                        thumbnailBitmap =FilesUtils.getVideoCover(info.path)
                        if(thumbnailBitmap!=null){
                            if(filesViewModel.thumbnailBitmapArray.size>filesViewModel.thumbnailsMaxNum){
                                val pair=filesViewModel.thumbnailBitmapArray.first()
                                filesViewModel.thumbnailBitmapArray.removeAt(0)
                                pair.second.recycle()
                            }
                            filesViewModel.thumbnailBitmapArray.add(Pair(info.path,thumbnailBitmap!!))
                        }
                    }
                    thumbnailBitmap?.prepareToDraw()
                }
            }
        }
    }
//    val lifecycleOwner = LocalLifecycleOwner.current
//    DisposableEffect(lifecycleOwner) {
//        onDispose {
//            thumbnailBitmap?.recycle()
//        }
//    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement= Arrangement.Center,
    ){
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .clickable {
                    val is_checked = !filesViewModel.filesState[info.path]!!.value
                    filesViewModel.updateFilesState(info.path, is_checked)
                    if (is_checked) {
                        filesViewModel.file = File(info.path)
                    } else {
                        filesViewModel.file = null
                    }
                }
        ){
            if(thumbnailBitmap!=null) {
                Image(
                    bitmap = thumbnailBitmap!!.asImageBitmap(),
                    modifier = Modifier
                        .width(128.dp)
                        .height(128.dp)
                        .background(color = Color.Black, shape = RoundedCornerShape(10.dp)),
                    contentDescription = null
                )
            }
            else{
                Icon(painter = painterResource(id = R.drawable.baseline_video_file_24),
                    tint = Color.Yellow,
                    modifier = Modifier
                        .width(128.dp)
                        .height(128.dp)
                        .background(color = Color.Black, shape = RoundedCornerShape(10.dp))
                    ,
                    contentDescription = null)
            }
            if(filesViewModel.filesState[info.path]!!.value) {
                Icon(painter =painterResource(R.drawable.baseline_check_24),
                    tint = Color.White,
                    modifier = Modifier
                        .padding(start = 64.dp, top = 64.dp)
                        .size(64.dp) ,
                    contentDescription =null)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        val file_name=info.name
        Text(
            text =info.name,
//            fontWeight = FontWeight.Bold,
            overflow = TextOverflow.Ellipsis,
            maxLines = 5,
            softWrap = true
        )
        Spacer(modifier = Modifier.height(50.dp))
//        if(file_name.length<=19) {
//            Text(text = file_name)
//        }
//        else{
//            Text(text =file_name.substring(0,11)+"..."+file_name.substring(file_name.length-5))
//        }
    }
}


@Composable
private fun ShowAudioFileInfo(
    filesViewModel: FilesViewModel,
    info: AudioInfo
){
    if(!filesViewModel.filesState.containsKey(info.path)){
        filesViewModel.filesState[info.path]= mutableStateOf(false)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement= Arrangement.Center,
    ){
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .clickable {
                    val is_checked = !filesViewModel.filesState[info.path]!!.value
                    filesViewModel.updateFilesState(info.path, is_checked)
                    if (is_checked) {
                        filesViewModel.file = File(info.path)
                    } else {
                        filesViewModel.file = null
                    }
                }
        ){
            Icon(painter = painterResource(id = R.drawable.baseline_audio_file_24),
                tint = Color.Green,
                modifier = Modifier
                    .width(128.dp)
                    .height(128.dp)
                    .background(color = Color.White, shape = RoundedCornerShape(10.dp))
                ,
                contentDescription = null)
            if(filesViewModel.filesState[info.path]!!.value) {
                Icon(painter =painterResource(R.drawable.baseline_check_24),
                    tint = Color.White,
                    modifier = Modifier
                        .padding(start = 64.dp, top = 64.dp)
                        .size(64.dp) ,
                    contentDescription =null)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        val file_name=info.name
        Text(
            text =info.name,
//            fontWeight = FontWeight.Bold,
            overflow = TextOverflow.Ellipsis,
            maxLines = 5,
            softWrap = true
        )
        Spacer(modifier = Modifier.height(50.dp))
//        if(file_name.length<=19) {
//            Text(text = file_name)
//        }
//        else{
//            Text(text =file_name.substring(0,11)+"..."+file_name.substring(file_name.length-5))
//        }
    }
}

