package com.example.audioandvideoeditor.components

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.entity.AudioInfo
import com.example.audioandvideoeditor.entity.VideoInfo
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.utils.FilesUtils
import com.example.audioandvideoeditor.utils.TextsUtils
import com.example.audioandvideoeditor.viewmodel.FilesViewModel2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File


private val TAG="FilesListScreen2"
@Composable
fun FilesListScreen2_20250606(
    videoPlay:(file: File, route:String)->Unit,
    filesViewModel: FilesViewModel2 = viewModel()
){
    val life= rememberLifecycle()
    life.onLifeCreate {
        filesViewModel.videoPlay=videoPlay
    }
    ShowListScreen(filesViewModel)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilesListScreen2(
    videoPlay:(file: File, route:String)->Unit,
    filesViewModel: FilesViewModel2 = viewModel()
){
    val life= rememberLifecycle()
    val context= LocalContext.current
    life.onLifeCreate {
        filesViewModel.videoPlay=videoPlay
        filesViewModel.setSortCriteriaAndOrder(ConfigsUtils.files_sort_flag)
    }
    filesViewModel.setContentResolver(LocalContext.current.contentResolver)
    showFileDetails(filesViewModel)
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

    Column(modifier = Modifier.fillMaxSize()) {
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
            val padding= PaddingValues(5.dp)
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                when(page){
                    0->VideosList(padding, filesViewModel)
                    1->AudiosList(padding, filesViewModel)
                    2->FilesList(padding, filesViewModel)
                }
                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f))
            }

        }
    }
}


@Composable
private fun ShowListScreen(
    filesViewModel: FilesViewModel2
){
    filesViewModel.setContentResolver(LocalContext.current.contentResolver)
    showFileDetails(filesViewModel)
    Scaffold(
        topBar ={
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 20.dp)
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

@Composable
private fun ShowList(padding: PaddingValues, filesViewModel: FilesViewModel2){
    when(filesViewModel.show_flag.value){
        0->VideosList(padding, filesViewModel)
        1->AudiosList(padding, filesViewModel)
        2->FilesList(padding, filesViewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilesList(
    padding: PaddingValues,
    filesViewModel: FilesViewModel2
){
    val life= rememberLifecycle()
    life.onLifeCreate {
        if(filesViewModel.parent==null){
            val file= File(Environment.getExternalStorageDirectory().path)
            file.listFiles()?.let {
                filesViewModel.filesList.addAll(it)
            }
            filesViewModel.parent= file
        }
    }
    var showSortOptionsDialog by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState() // 1. Create LazyListState
    val coroutineScope = rememberCoroutineScope() // 2. Create CoroutineScope
    // 3. Launch a LaunchedEffect to observe sorting changes and scroll
    LaunchedEffect(filesViewModel.sortCriteria, filesViewModel.sortOrder) {
        // Only scroll if the list is not empty
        if (filesViewModel.displayFilesList.isNotEmpty()) {
            coroutineScope.launch {
                lazyListState.animateScrollToItem(0) // Animate scroll to the first item
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
                    },
                ) {
                    Spacer(modifier = Modifier.width(20.dp))
                    Icon(painter = painterResource(id = R.drawable.baseline_folder_24), contentDescription = null)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text="..")
                }
                IconButton(
                    onClick = {
                        showSortOptionsDialog = true
                    },
                    modifier =  Modifier.wrapContentSize(Alignment.TopEnd)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.sort_24px),
                        modifier=Modifier.size(48.dp),
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
            items(count =filesViewModel.displayFilesList.size){
                ShowFile(filesViewModel,it)
            }
        }
    }

    BackHandler(enabled =filesViewModel.backHandler_flag ) {
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
        Log.d(TAG,"BackHandler")
        filesViewModel.backHandler_flag=(filesViewModel.show_flag.value==2 &&filesViewModel.parent != null && filesViewModel.parent!!.path != Environment.getExternalStorageDirectory().path)
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
                                onClick = { sortCriteria2 = 0 }
                            )
                            Text(stringResource(id = R.string.file_name))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected =sortCriteria2== 1,
                                onClick = { sortCriteria2=1}
                            )
                            Text(stringResource(id = R.string.size))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = sortCriteria2== 2,
                                onClick = { sortCriteria2=2}
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
                            onCheckedChange = {
                                sortOrder2= if (sortOrder2 == 1) 0 else 1
                            }
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
    filesViewModel: FilesViewModel2,
    id:Int
){
    val file=filesViewModel.displayFilesList[id]
    var showMenu by remember { mutableStateOf(false) }
    val context= LocalContext.current
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (file.isFile) {
                    FilesUtils.openLocalFile(context, file)
                } else if (file.isDirectory) {
                    val dir = file
                    if (dir.listFiles() != null) {
                        filesViewModel.filesList.clear()
                        filesViewModel.parent = dir
                        filesViewModel.backHandler_flag =
                            (filesViewModel.show_flag.value == 2 && filesViewModel.parent != null && filesViewModel.parent!!.path != Environment.getExternalStorageDirectory().path)
//                        Log.d(TAG,"${filesViewModel.show_flag.value}  ${filesViewModel.parent!!.path}")
                        dir
                            .listFiles()
                            ?.let { it1 ->
                                filesViewModel.filesList.addAll(it1)
                            }
                    }
                }
            }
    ){
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if(file.isFile){
                Icon(
                    painter = painterResource(id = R.drawable.baseline_insert_drive_file_24),
                    contentDescription = null
                )
            }
            else{
                Icon(painter = painterResource(id = R.drawable.baseline_folder_24), contentDescription = null)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                modifier = Modifier.width(
                    (LocalConfiguration.current.screenWidthDp-250).dp
                ),
                text =file.name,
//                    fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis,
                maxLines = 5,
                softWrap = true
            )
            Spacer(modifier = Modifier.width(16.dp))
            // Kebab Menu (Three vertical dots)
            Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.semantics { contentDescription = "More options for ${file.name}" }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.info)) },
                        onClick = {
                            filesViewModel.file=file
                            filesViewModel.show_details_flag.value=3
                            showMenu = false
                        }
                    )
//                    if(file.isFile) {
//                        DropdownMenuItem(
//                            text = { Text(stringResource(id = R.string.open)) },
//                            onClick = {
//                                //onOpenFileClick(file)
//                                FilesUtils.openLocalFile(context,file)
//                                showMenu = false
//                            }
//                        )
//                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Divider(color = Color.LightGray, thickness = 1.dp)
    }
}


@Composable
private fun ShowFile_bf(
    filesViewModel: FilesViewModel2,
    id:Int
){
    val file=filesViewModel.displayFilesList[id]
    if(file.isFile){
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_insert_drive_file_24),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(20.dp))
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
                TextButton(onClick = {
                    filesViewModel.file=file
                    filesViewModel.show_details_flag.value=3
                }) {
                    Text(text= LocalContext.current.getString(R.string.info))
                }
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
                            }
                    }
                }
        ){
            Icon(painter = painterResource(id = R.drawable.baseline_folder_24), contentDescription = null)
            Spacer(modifier = Modifier.width(20.dp))
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
                TextButton(onClick = {
                    filesViewModel.file=file
                    filesViewModel.show_details_flag.value=3
                }) {
                    Text(text= LocalContext.current.getString(R.string.info))
                }
            }
        }
    }
}

@Composable
private fun ShowFile2_20250726(
    filesViewModel: FilesViewModel2,
    id:Int
){
    val file=filesViewModel.displayFilesList[id]
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
                    modifier = Modifier.width(
                        (LocalConfiguration.current.screenWidthDp-250).dp
                    ),
                    text =file.name,
//                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 5,
                    softWrap = true
                )
                Spacer(modifier = Modifier.width(5.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ){
                    TextButton(onClick = {
                        filesViewModel.file=file
                        filesViewModel.show_details_flag.value=3
                    }) {
                        Text(text= LocalContext.current.getString(R.string.info))
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    val context=LocalContext.current
                    TextButton(onClick = {
                        FilesUtils.openLocalFile(context,file)
                    }) {
                        Text(text= LocalContext.current.getString(R.string.open))
                        Spacer(modifier = Modifier.width(5.dp))
                    }
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
//                        Log.d(TAG,"${filesViewModel.show_flag.value}  ${filesViewModel.parent!!.path}")
                        dir
                            .listFiles()
                            ?.let { it1 ->
                                filesViewModel.filesList.addAll(it1)
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
                    modifier = Modifier.width(
                        (LocalConfiguration.current.screenWidthDp*3/5).dp
                    ),
                    text =file.name,
//                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 5,
                    softWrap = true
                )
                Spacer(modifier = Modifier.width(5.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ){
                    TextButton(onClick = {
                        filesViewModel.file=file
                        filesViewModel.show_details_flag.value=3
                    }) {
                        Text(text= LocalContext.current.getString(R.string.info))
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
        }
    }
}

@Composable
private fun VideosList(
    padding: PaddingValues,
    filesViewModel: FilesViewModel2
){
    val videosPager=filesViewModel.videosPager
    val lazyPagingItems = videosPager.flow.collectAsLazyPagingItems()
    LazyVerticalGrid (
        columns = GridCells.Adaptive(minSize = 128.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                ShowVideoFileInfo(item,filesViewModel)
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
private fun AudiosList(
    padding: PaddingValues,
    filesViewModel: FilesViewModel2
){
    val audiosPager=filesViewModel.audiosPager
    val lazyPagingItems = audiosPager.flow.collectAsLazyPagingItems()
    LazyVerticalGrid (
        columns = GridCells.Adaptive(minSize = 128.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                ShowAudioFileInfo(item,filesViewModel)
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
    filesViewModel:FilesViewModel2
){
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
                            thumbnailBitmap = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                                FilesUtils.getThumbnail(context.contentResolver,info.uri)
                            }else{
                                FilesUtils.getVideoCover(info.path)
                            }
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
//    val  bitmap= FilesUtils.getThumbnail(LocalContext.current.contentResolver,info.uri)
//    bitmap?.prepareToDraw()

// Use Coil to create and display a thumbnail of a video or image with a specific height
// ImageLoader has its own memory and storage cache, and this one is configured to also
// load frames from videos
//    val videoEnabledLoader = ImageLoader.Builder(context)
//        .components {
//            add(VideoFrameDecoder.Factory())
//        }.build()
//// Coil requests images that match the size of the AsyncImage composable, but this allows
//// for precise control of the height
//    val request = ImageRequest.Builder(context)
//        .data(info.uri)
//        .size(Int.MAX_VALUE, THUMBNAIL_HEIGHT)
//        .build()
//    AsyncImage(
//        model = request,
//        imageLoader = videoEnabledLoader,
//        modifier = Modifier
//            .clip(RoundedCornerShape(20))    ,
//        contentDescription = null
//    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement= Arrangement.Center,
        modifier = Modifier.background(
            color =  Color(0xFFFFDBD1)
            , shape=RoundedCornerShape(10.dp))
    ){
        Spacer(modifier = Modifier.height(5.dp))
        if(thumbnailBitmap!=null) {
            Image(
                bitmap = thumbnailBitmap!!.asImageBitmap(),
                modifier = Modifier
                    .width(128.dp)
                    .height(128.dp)
                    .background(color = Color.Black, shape = RoundedCornerShape(10.dp))
                    .clickable {
                        val file = File(info.path)
                        filesViewModel.videoPlay(file, VideoPlay.route)
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
                    .background(color = Color.Black, shape = RoundedCornerShape(10.dp))
                    .clickable {
                        val file = File(info.path)
                        filesViewModel.videoPlay(file, VideoPlay.route)
                    }
                ,
                contentDescription = null)
        }
        Spacer(modifier = Modifier.height(5.dp))
//        if(info.name.length<=19) {
//            Text(text = info.name, modifier = Modifier.height(45.dp))
//        }
//        else{
//            Text(text =info.name.substring(0,11)+"..."+info.name.substring(info.name.length-5)
//            , modifier = Modifier.height(45.dp)
//            )
//        }
        Text(
            text =info.name,
//            fontWeight = FontWeight.Bold,
            overflow = TextOverflow.Ellipsis,
            maxLines = 5,
            softWrap = true
        )
        TextButton(onClick = {
            filesViewModel.videoInfo=info
            filesViewModel.show_details_flag.value=1
        }) {
            Text(text= LocalContext.current.getString(R.string.info))
        }
    }
}

@Composable
private fun ShowAudioFileInfo(
    info: AudioInfo,
    filesViewModel:FilesViewModel2
){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement= Arrangement.Center,
        modifier = Modifier.background(
            color =  Color(0xFFFFDBD1)
            , shape=RoundedCornerShape(10.dp))
    ){
        Spacer(modifier = Modifier.height(5.dp))
        Icon(painter = painterResource(id = R.drawable.baseline_audio_file_24),
            tint = Color.Green,
            modifier = Modifier
                .width(128.dp)
                .height(128.dp)
                .background(color = Color.White, shape = RoundedCornerShape(10.dp))
                .clickable {
                    val file = File(info.path)
                    filesViewModel.videoPlay(file, VideoPlay.route)
                }
            ,
            contentDescription = null)
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text =info.name,
//            fontWeight = FontWeight.Bold,
            overflow = TextOverflow.Ellipsis,
            maxLines = 5,
            softWrap = true
        )
//        if(info.name.length<=19) {
//            Text(text = info.name, modifier = Modifier.height(45.dp))
//        }
//        else{
//            Text(text =info.name.substring(0,11)+"..."+info.name.substring(info.name.length-5)
//                , modifier = Modifier.height(45.dp)
//            )
//        }
        TextButton(onClick = {
            filesViewModel.audioInfo=info
            filesViewModel.show_details_flag.value=2
        }) {
            Text(text= LocalContext.current.getString(R.string.info))
        }
    }
}



@Composable
fun showFileDetails(
    filesViewModel: FilesViewModel2
){
when(filesViewModel.show_details_flag.value){
    1->{
        AlertDialog(
            onDismissRequest ={
                filesViewModel.show_details_flag.value=0
            },
            title ={
                Text(LocalContext.current.getString(R.string.info))
            },
            text = {
                val ctx= LocalContext.current
                if (filesViewModel.videoInfo != null) {
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("${LocalContext.current.getString(R.string.type)}:${FilesUtils.getTypeFromPath(filesViewModel.videoInfo!!.path)}")
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("${LocalContext.current.getString(R.string.duration)}:${filesViewModel.videoInfo!!.duration/1000}")
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "${LocalContext.current.getString(R.string.size)}:${
                                TextsUtils.getSizeText(
                                    filesViewModel.videoInfo!!.size
                                )
                            }"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("${LocalContext.current.getString(R.string.path)}:${filesViewModel.videoInfo!!.path}")
                        Text(
                            text=LocalContext.current.getString(R.string.copy)+LocalContext.current.getString(R.string.path),
                            color = Color.Red,
                            modifier = Modifier.clickable {
                                FilesUtils.copyStr(filesViewModel.videoInfo!!.path,ctx)
                                sendToast(ctx, ctx.getString(R.string.copy_to_clipboard))
                            }
                        )
                    }
                }
            },
            confirmButton ={
                TextButton(onClick = {
                    filesViewModel.show_details_flag.value=0
                }) {
                    Text(LocalContext.current.getString(R.string.ok))
                }
            },
            dismissButton ={},
        )
    }
    2->{
        AlertDialog(
            onDismissRequest ={
                filesViewModel.show_details_flag.value=0
            },
            title ={
                Text(LocalContext.current.getString(R.string.info))
            },
            text = {
                val ctx= LocalContext.current
                if (filesViewModel.audioInfo != null) {
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("${LocalContext.current.getString(R.string.type)}:${FilesUtils.getTypeFromPath(filesViewModel.audioInfo!!.path)}")
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("${LocalContext.current.getString(R.string.duration)}:${filesViewModel.audioInfo!!.duration/1000}")
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "${LocalContext.current.getString(R.string.size)}:${
                                TextsUtils.getSizeText(
                                    filesViewModel.audioInfo!!.size
                                )
                            }"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("${LocalContext.current.getString(R.string.path)}:${filesViewModel.audioInfo!!.path}")
                        Text(
                            text=LocalContext.current.getString(R.string.copy)+LocalContext.current.getString(R.string.path),
                            color = Color.Red,
                            modifier = Modifier.clickable {
                                FilesUtils.copyStr(filesViewModel.audioInfo!!.path,ctx)
                                sendToast(ctx, ctx.getString(R.string.copy_to_clipboard))
                            }
                        )
                    }
                }
            },
            confirmButton ={
                TextButton(onClick = {
                    filesViewModel.show_details_flag.value=0
                }) {
                    Text(LocalContext.current.getString(R.string.ok))
                }
            },
            dismissButton ={},
        )
    }
    3->{
        AlertDialog(
            onDismissRequest ={
                filesViewModel.show_details_flag.value=0
            },
            title ={
               Text(LocalContext.current.getString(R.string.info))
            },
            text = {
                val ctx= LocalContext.current
                if (filesViewModel.file != null) {
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        if(filesViewModel.file!!.isFile){
                            Text("${LocalContext.current.getString(R.string.type)}:${filesViewModel.file!!.extension}")
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "${LocalContext.current.getString(R.string.size)}:${
                                    TextsUtils.getSizeText(
                                        filesViewModel.file!!.length()
                                    )
                                }"
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        Text("${LocalContext.current.getString(R.string.path)}:${filesViewModel.file!!.path}")
                        Text(
                            text=LocalContext.current.getString(R.string.copy)+LocalContext.current.getString(R.string.path),
                            color = Color.Red,
                            modifier = Modifier.clickable {
                                FilesUtils.copyStr(filesViewModel.file!!.path,ctx)
                                sendToast(ctx, ctx.getString(R.string.copy_to_clipboard))
                            }
                        )
                    }
                }
            },
            confirmButton ={
               TextButton(onClick = {
                   filesViewModel.show_details_flag.value=0
               }) {
                   Text(LocalContext.current.getString(R.string.ok))
               }
            },
            dismissButton ={},
        )
    }
}
}

private  fun sendToast(ctx: Context, text:String){
    val toast = Toast.makeText( ctx, text, Toast.LENGTH_SHORT)
    toast.setGravity(Gravity.CENTER, 0, 0)
    toast.show()
}