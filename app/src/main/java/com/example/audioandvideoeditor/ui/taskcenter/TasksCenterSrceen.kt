package com.example.audioandvideoeditor.ui.taskcenter


import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.entity.Task
import com.example.audioandvideoeditor.navigation.Destination
import com.example.audioandvideoeditor.utils.TextsUtils
import kotlinx.coroutines.launch

/**
 * 任务中心主页面（最终优化版）
 * 无Activity依赖、无Dao依赖、自动响应任务状态切换
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TasksCenterScreen(
    readContext:(path_or_uri:String, route:String,flag:Boolean)->Unit,
    viewModel: TasksCenterViewModel = viewModel()
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部Tab栏
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text(stringResource(R.string.tab_running)) }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text(stringResource(R.string.tab_waiting)) }
            )
            Tab(
                selected = pagerState.currentPage == 2,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                text = { Text(stringResource(R.string.tab_history)) }
            )
        }

        // 页面内容
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> RunningTaskList(readContext,viewModel = viewModel)
                1 -> WaitingTaskList(readContext,viewModel = viewModel)
                2 -> HistoryTaskList(readContext,viewModel = viewModel)
            }
        }
    }

    // 日志弹窗
//    TaskLogDialog(viewModel = viewModel)

    // 页面销毁时清理
    DisposableEffect(Unit) {
        onDispose { /* ViewModel已自动清理 */ }
    }
}

// ==================== 运行中任务列表 ====================
@Composable
private fun RunningTaskList(
    readContext:(path_or_uri:String, route:String,flag:Boolean)->Unit,
    viewModel: TasksCenterViewModel
) {
    val runningTasks = viewModel.runningTasks

    if (runningTasks.isEmpty()) {
        EmptyTaskTip(stringResource(R.string.tip_empty_running))
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(runningTasks, key = { it.task_id }) { task ->
            TaskItem(
                task = task,
                progress = viewModel.getTaskProgress(task.task_id),
                showProgress = true,
                showCancel = true,
                showPlay = false,
                showDelete = false,
                onCancelClick = { viewModel.cancelTask(task.task_id) },
                onLogClick = {
//                    viewModel.readTaskLog(task.log_path)
                    readContext(
                        if(task.type!=2){
                            task.log_path
                        }
                        else{
                            task.path
                        }
                        ,
                        Destination.FileRead.route,
                        true)
                             },
                onPlayClick = {},
                onDeleteClick = {}
            )
        }
    }
}

// ==================== 等待中任务列表 ====================
@Composable
private fun WaitingTaskList(
    readContext:(path_or_uri:String, route:String,flag:Boolean)->Unit,
    viewModel: TasksCenterViewModel
) {
    val waitingTasks = viewModel.waitingTasks

    if (waitingTasks.isEmpty()) {
        EmptyTaskTip(stringResource(R.string.tip_empty_waiting))
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(waitingTasks, key = { it.task_id }) { task ->
            TaskItem(
                task = task,
                progress = 0f,
                showProgress = false,
                showCancel = true,
                showPlay = false,
                showDelete = false,
                onCancelClick = { viewModel.cancelTask(task.task_id) },
                onLogClick = {
                },
                onPlayClick = {},
                onDeleteClick = {}
            )
        }
    }
}

// ==================== 历史任务列表（完成/取消/失败） ====================
@Composable
private fun HistoryTaskList(
    readContext:(path_or_uri:String, route:String,flag:Boolean)->Unit,
    viewModel: TasksCenterViewModel
) {
    val historyTasks = viewModel.historyTasks
    val context = LocalContext.current

    if (historyTasks.isEmpty()) {
        EmptyTaskTip(stringResource(R.string.tip_empty_history))
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(historyTasks, key = { it.task_id }) { task ->
            TaskItem(
                task = task,
                progress = 1f,
                showProgress = false,
                showCancel = false,
                showPlay =
                    if(task.type==2){
                        false
                    }
                    else{
                        true
                    }
                ,
                showDelete = true,
                onCancelClick = {},
                onLogClick = {
//                    viewModel.readTaskLog(
//                        if(task.type==2){
//                            task.path
//                        }
//                        else{
//                            task.log_path
//                        }
//                        )
                    readContext(
                        task.log_path
                        ,
                        Destination.FileRead.route,
                        false)
                },
                onPlayClick = {
                    // 播放视频/音频（系统默认播放器）
//                    val intent = Intent(Intent.ACTION_VIEW).apply {
//                        setDataAndType(Uri.parse(task.path), "video/*")
//                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                    }
//                    context.startActivity(intent)
                    if(task.path.isNotEmpty()||task.uri.isNotEmpty()){
                        readContext(
                            if(task.type!=4)
                            {task.path}
                            else
                            {task.uri},
                            Destination.VideoPlay.route,
                            false
                        )
                    }

                },
                onDeleteClick = {
                    viewModel.taskToDelete=task
                    viewModel.showDeleteDialog.value=true
                }
            )
        }
    }

    if (viewModel.showDeleteDialog.value && viewModel.taskToDelete !=null) {
        // 0 = 仅删除记录, 1 = 删除记录及文件
        var deleteOption by remember { mutableStateOf(1) }
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteDialog.value = false },
            title = { Text(stringResource(R.string.delete_task_title)) },
            text = {
                Column {
                    // 提示文本
                    Text(
                        stringResource(
                            R.string.delete_task_confirm_message,
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    //类型2 ffmpege命令行 仅支持删除任务
                    if(viewModel.taskToDelete!!.type!=2){
                    // 选项1：仅删除记录
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteOption = 0 }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = deleteOption == 0,
                            onClick = { deleteOption = 0 }
                        )
                        Text(
                            text = stringResource(R.string.delete_record_only),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    // 选项2：删除记录及文件
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteOption = 1 }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = deleteOption == 1,
                            onClick = { deleteOption = 1 }
                        )
                        Text(
                            text = stringResource(R.string.delete_with_file),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (deleteOption == 1) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                    }
                    }
                    else{
                        deleteOption = 0
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deleteOption = 0 }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = deleteOption == 0,
                                onClick = {  }
                            )
                            Text(
                                text = stringResource(R.string.delete_record_only),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 根据选中的选项执行删除
                        viewModel.deleteTask(
                            deleteFile = (deleteOption == 1)  // 只有选项2才删文件
                        )
                        viewModel.showDeleteDialog.value = false
                    }
                ) {
                    Text(stringResource(R.string.ok))  // 复用已有的 "确定" 字符串
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDeleteDialog.value= false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

// ==================== 通用任务Item组件（核心复用） ====================
@Composable
private fun TaskItem(
    task: Task,
    progress: Float,
    showProgress: Boolean,
    showCancel: Boolean,
    showPlay: Boolean,
    showDelete: Boolean,
    onCancelClick: () -> Unit,
    onLogClick: () -> Unit,
    onPlayClick: () -> Unit,
    onDeleteClick:()-> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 任务名称
            Text(
                text = task.file_name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 任务状态 + 进度
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getTaskStatusText(task.status,LocalContext.current),
                    modifier = Modifier.weight(1f),
                    color = getTaskStatusColor(task.status)
                )

                // 进度条（仅运行中显示）
                if (showProgress) {
                    if(task.type!=2){
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.width(120.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "${(progress * 100).toInt()}%")
                    }
                    else{
                        Text(text = TextsUtils.millisecondsToString(progress.toLong()))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // 播放按钮
                if (showPlay) {
                    TextButton(onClick = onPlayClick) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.play),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.play))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if(task.type!=4 && task.status!=3){
                    // 日志按钮
                    TextButton(onClick = onLogClick) {
                        Text(stringResource(R.string.log))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                // 取消按钮
                if (showCancel) {
                    TextButton(onClick = onCancelClick) {
                        Text(stringResource(R.string.cancel), color = Color.Red)
                    }
                }
                if (showDelete) {
                    TextButton(onClick = onDeleteClick) {
                        Text(stringResource(R.string.delete), color = Color.Red)
                    }
                }
            }
        }
    }
}

// ==================== 日志弹窗 ====================
@Composable
private fun TaskLogDialog(viewModel: TasksCenterViewModel) {
    val showDialog = viewModel.showLogDialog.value
    val logList = viewModel.logContent

    AnimatedVisibility(visible = showDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.closeLog() },
            title = { Text(stringResource(R.string.label_task_log)) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Top
                ) {
                    items(logList) { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.closeLog() }) {
                    Text(stringResource(R.string.btn_close))
                }
            },
            modifier = Modifier.fillMaxWidth(0.9f)
        )
    }
}

// ==================== 空数据提示 ====================
@Composable
private fun EmptyTaskTip(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
    }
}

// ==================== 工具方法：任务状态文本/颜色 ====================
private fun getTaskStatusText(status: Int,context: Context): String {
    return when (status) {
        0 -> context.getString(R.string.tab_running)
        1 -> context.getString(R.string.status_completed)
        2 -> context.getString(R.string.cancelled)
        3 ->context.getString(R.string.wait)
        -1 -> context.getString(R.string.status_failed)
        else -> context.getString(R.string.status_unknown_state)
    }
}

private fun getTaskStatusColor(status: Int): Color {
    return when (status) {
        0 -> Color(0xFF1976D2) // 运行中-蓝
        1 -> Color(0xFF388E3C) // 已完成-绿
        2 -> Color(0xFF757575) // 已取消-灰
        3 -> Color(0xFFFFA000) // 等待中-琥珀黄
        -1 -> Color(0xFFD32F2F) // 失败-红
        else -> Color.Black
    }
}