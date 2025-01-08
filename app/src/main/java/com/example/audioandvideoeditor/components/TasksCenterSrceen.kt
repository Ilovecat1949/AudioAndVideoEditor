package com.example.audioandvideoeditor.components

import android.util.Log
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audioandvideoeditor.MainActivity
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.utils.FilesUtils
import com.example.audioandvideoeditor.viewmodel.TasksCenterViewModel
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.File
import java.io.FileReader


private val TAG="TasksCenterScreen"
@Composable
fun TasksCenterScreen(
    activity: MainActivity,
    videoPlay:(file: File, route:String)->Unit,
    tasksCenterViewModel: TasksCenterViewModel= viewModel()
){

    val life= rememberLifecycle()
    life.onLifeCreate {
        if(activity.tasks_binder_flag){
            tasksCenterViewModel.tasksBinder=activity.tasksBinder
            tasksCenterViewModel.tasks_binder_flag.value=activity.tasks_binder_flag
        }
        else{
            tasksCenterViewModel.tasks_binder_flag.value=false
        }

        tasksCenterViewModel.tasksDao=activity.tasksDao
        tasksCenterViewModel.refresh_flag=false
//        tasksCenterViewModel.initlist_flag=true
        tasksCenterViewModel.videoPlay=videoPlay
        Log.d(TAG,"TasksCenterScreen onLifeCreate")
    }
    if(tasksCenterViewModel.tasks_binder_flag.value) {
        Scaffold(
            topBar = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
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
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = LocalContext.current.resources.getString(R.string.executing))
                        Checkbox(
                            checked = (tasksCenterViewModel.show_flag.value == 0),
                            onCheckedChange = {
                                if (it) {
                                    tasksCenterViewModel.show_flag.value = 0
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = LocalContext.current.resources.getString(R.string.waiting_for_execution))
                        Checkbox(
                            checked = (tasksCenterViewModel.show_flag.value == 1),
                            onCheckedChange = {
                                if (it) {
                                    tasksCenterViewModel.show_flag.value = 1
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
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
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = LocalContext.current.resources.getString(R.string.cancel))
                        Checkbox(
                            checked = (tasksCenterViewModel.show_flag.value == 2),
                            onCheckedChange = {
                                if (it) {
                                    tasksCenterViewModel.show_flag.value = 2
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = LocalContext.current.resources.getString(R.string.execution_failed))
                        Spacer(modifier = Modifier.width(10.dp))
                        Checkbox(
                            checked = (tasksCenterViewModel.show_flag.value == 3),
                            onCheckedChange = {
                                if (it) {
                                    tasksCenterViewModel.show_flag.value = 3
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
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
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = LocalContext.current.resources.getString(R.string.end_of_execution))
                        Checkbox(
                            checked = (tasksCenterViewModel.show_flag.value == 4),
                            onCheckedChange = {
                                if (it) {
                                    tasksCenterViewModel.show_flag.value = 4
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }
            })
        { paddingValues ->
            ShowList(paddingValues, tasksCenterViewModel)
        }
    }
    LaunchedEffect(true){
        if(!tasksCenterViewModel.tasks_binder_flag.value){
            while (true){
                if(activity.tasks_binder_flag){
                    tasksCenterViewModel.tasksBinder=activity.tasksBinder
                    tasksCenterViewModel.tasks_binder_flag.value=activity.tasks_binder_flag
                    break
                }
                else{
                    tasksCenterViewModel.tasks_binder_flag.value=false
                }
                delay(100)
            }
        }
        if(tasksCenterViewModel.tasks_binder_flag.value) {
            var i = 0
            while (true) {
                if (activity.tasksBinder.getRemainingTasksNum() == 0) {
                    i++
                } else {
                    i = 0
                }
                if (i > 10) {
                    break
                }
                tasksCenterViewModel.reFresh()
                delay(1000)
            }
        }
    }
}
@Composable
private fun ShowList(padding: PaddingValues,tasksCenterViewModel: TasksCenterViewModel){
    when(tasksCenterViewModel.show_flag.value){
        0->RunningTasksList(padding, tasksCenterViewModel)
        1->WatingTasksList(padding, tasksCenterViewModel)
        2->CancelledTasksList(padding , tasksCenterViewModel )
        3->FailedTasksList(padding , tasksCenterViewModel)
        4-> EndedTasksList(padding , tasksCenterViewModel )
    }

}
@Composable
private fun RunningTasksList(
    padding: PaddingValues,
    tasksCenterViewModel: TasksCenterViewModel
){
    LazyColumn(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = padding.calculateTopPadding() + 40.dp)
    ){
        val size=tasksCenterViewModel.runningTasksList.size
        items(
        count=tasksCenterViewModel.runningTasksList.size,
        key={
            tasksCenterViewModel.runningTasksList[it].long_arr[0]
        }
       ){
//            Log.d(TAG,"size:${size}")
            val path=tasksCenterViewModel.runningTasksList[it].str_arr[0]
            val id=tasksCenterViewModel.runningTasksList[it].long_arr[0]
            val name=FilesUtils.getNameFromPath(path)
            if(!tasksCenterViewModel.tasksState.containsKey(id)){
                tasksCenterViewModel.tasksState[id]= mutableStateOf("0%")
            }
            tasksCenterViewModel.show_log_flag_map[path]= mutableStateOf(false)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth()
            ) {
                if(name.length<25) {
                    Text(text = name)
                }
                else{
                    Text(text =name.substring(0,19)+"..."+name.substring(name.length-5))
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ){
                    if(!name.endsWith(".log")) {
                        TextButton(onClick = {
                            tasksCenterViewModel.cancelTask(id)
                        }) {
                            Text(text = LocalContext.current.resources.getString(R.string.cancel))
                        }
                        showRunningTaskState(tasksCenterViewModel, id)
                    }
                    else{
                        TextButton(onClick = {
                            tasksCenterViewModel.show_log_flag_map[path]!!.value=true
                        }) {
                            Text(text = LocalContext.current.getString(R.string.log))
                        }
                    }
                }
            }
            readLog(path,tasksCenterViewModel)
       }
    }
}
@Composable
private fun showRunningTaskState(tasksCenterViewModel: TasksCenterViewModel,id:Long){
//    Log.d(TAG,"id:${id}")
    Text(text=tasksCenterViewModel.tasksState[id]!!.value,
         modifier = Modifier.width(80.dp)
    )
}
@Composable
private fun WatingTasksList(
    padding: PaddingValues,
    tasksCenterViewModel: TasksCenterViewModel
){
    LazyColumn(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = padding.calculateTopPadding() + 40.dp)
    ){
        items(
            count=tasksCenterViewModel.watingTasksList.size,
            key={
                tasksCenterViewModel.watingTasksList[it].long_arr[0]
            }
        ){
            val path=tasksCenterViewModel.watingTasksList[it].str_arr[0]
            val name=FilesUtils.getNameFromPath(path)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth()
            ) {
                if(name.length<25) {
                    Text(text = name)
                }
                else{
                    Text(text =name.substring(0,19)+"..."+name.substring(name.length-5))
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ){
                    Text(text= LocalContext.current.resources.getString(R.string.waiting_for_execution))
                }
            }
        }
    }
}

@Composable
private fun CancelledTasksList(
    padding: PaddingValues,
    tasksCenterViewModel: TasksCenterViewModel
){
    LazyColumn(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = padding.calculateTopPadding() + 40.dp)
    ){
        items(
            count=tasksCenterViewModel.cancelledTasksList.size,
            key={
                tasksCenterViewModel.cancelledTasksList[it].long_arr[0]
            }
        ){
            val path=tasksCenterViewModel.cancelledTasksList[it].str_arr[0]
            val name=FilesUtils.getNameFromPath(path)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth()
            ) {
                if(name.length<25) {
                    Text(text = name)
                }
                else{
                    Text(text =name.substring(0,19)+"..."+name.substring(name.length-5))
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ){
                    Text(text=LocalContext.current.resources.getString(R.string.cancelled))
                }
            }
        }
    }
}

@Composable
private fun FailedTasksList(
    padding: PaddingValues,
    tasksCenterViewModel: TasksCenterViewModel){
    LazyColumn(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = padding.calculateTopPadding() + 40.dp)
    ){
        items(
            count=tasksCenterViewModel.failedTasksList .size,
            key={
                tasksCenterViewModel.failedTasksList[it].long_arr[0]
            }
        ) {
            val path=tasksCenterViewModel.endedTasksList[it].str_arr[0]
            val name=FilesUtils.getNameFromPath(path)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth()
            ) {
                if (name.length < 25) {
                    Text(text = name)
                } else {
                    Text(text = name.substring(0, 19) + "..." + name.substring(name.length - 5))
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = LocalContext.current.resources.getString(R.string.fail))
                }
            }
        }
    }
}

@Composable
private fun EndedTasksList(
    padding: PaddingValues,
    tasksCenterViewModel: TasksCenterViewModel
){
    LazyColumn(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = padding.calculateTopPadding() + 40.dp)
    ){
        items(
            count=tasksCenterViewModel.endedTasksList .size,
            key={
                tasksCenterViewModel.endedTasksList[it].long_arr[0]
            }
        ) {
            val path=tasksCenterViewModel.endedTasksList[it].str_arr[0]
            val name=FilesUtils.getNameFromPath(path)
            tasksCenterViewModel.show_log_flag_map[path]= mutableStateOf(false)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth()
            ) {
                if (name.length < 25) {
                    Text(text = name)
                } else {
                    Text(text = name.substring(0, 19) + "..." + name.substring(name.length - 5))
                }
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
//                    TextButton(onClick = {
//                        val file=File(path)
//                        tasksCenterViewModel.videoPlay(file,VideoPlay.route)
//                    }) {
//                        Text(text=LocalContext.current.resources.getString(R.string.play))
//                    }
                        if(!name.endsWith(".log")) {
                            Icon(
                                painter = painterResource(id = R.drawable.play_circle_24px),
                                tint = Color.Black,
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(24.dp)
                                    .background(color = Color.Transparent)
                                    .clickable {
                                        val file = File(path)
                                        tasksCenterViewModel.videoPlay(file, VideoPlay.route)
                                    },
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        else{
                           TextButton(onClick = {
                               tasksCenterViewModel.show_log_flag_map[path]!!.value=true
                           }) {
                               Text(text=LocalContext.current.getString(R.string.log))
                           }
                        }
                        Text(text = LocalContext.current.resources.getString(R.string.ended))
                }
            }
            readLog(path,tasksCenterViewModel)
        }
    }
}

@Composable
private fun readLog(
    path:String,
    tasksCenterViewModel: TasksCenterViewModel
){
    if(tasksCenterViewModel.show_log_flag_map[path]!!.value){
       val file=File(path)
       if(file.exists()&&file.canRead()){
           tasksCenterViewModel.file=file
           val reader = FileReader(file)
           val bufferedReader = BufferedReader(reader)
           tasksCenterViewModel.bufferedReader=bufferedReader
           tasksCenterViewModel.readLogFile()
           AlertDialog(
               onDismissRequest = {
                   tasksCenterViewModel.show_log_flag_map[path]!!.value=false
                   bufferedReader.close()
                                  },
               title ={Text(text=LocalContext.current.getString(R.string.log))},
               text ={
//                   val life= rememberLifecycle()
//                   life.onLifeDestroy{
//                       bufferedReader.close()
//                       Log.d("AlertDialog","life  onLifeDestroy")
//                   }
                   Row (
                       modifier = Modifier
                           .fillMaxWidth()
                           .height(50.dp),
                       verticalAlignment=Alignment.CenterVertically
                   ){
                       TextButton(onClick = {
                           tasksCenterViewModel.readLogFile()
                       }) {
                           Text(text = LocalContext.current.getString(R.string.loading))
                           Icon(painter = painterResource(id = R.drawable.baseline_cached_24) , 
                               contentDescription = null)
                       }
                   }
                   val scrollState = rememberLazyListState()
                   LazyColumn(
                       verticalArrangement = Arrangement.Center,
                       horizontalAlignment = Alignment.Start,
                       contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                       modifier = Modifier
                           .padding(top = 70.dp)
                           .fillMaxWidth(),
                       state=scrollState
                   ){
                     items(
                         count = tasksCenterViewModel.log_lines.size
                     ){
                         Text(text = tasksCenterViewModel.log_lines[it])
                     }
                   }
               },
               confirmButton ={
                  TextButton(onClick = {
                      tasksCenterViewModel.show_log_flag_map[path]!!.value=false
                      bufferedReader.close()
                  }) {
                      Text(text="确定")
                  }
               },
               dismissButton ={},
       )
       }
    }
}