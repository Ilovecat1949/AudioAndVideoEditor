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
import androidx.compose.material3.Checkbox
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
import java.io.File


private val TAG="TasksCenterScreen"
@Composable
fun TasksCenterScreen(
    activity: MainActivity,
    videoPlay:(file: File, route:String)->Unit,
    tasksCenterViewModel: TasksCenterViewModel= viewModel()
){
    tasksCenterViewModel.tasksBinder=activity.tasksBinder
    tasksCenterViewModel.tasksDao=activity.tasksDao
    val life= rememberLifecycle()
    life.onLifeCreate {
        tasksCenterViewModel.refresh_flag=false
//        tasksCenterViewModel.initlist_flag=true
        tasksCenterViewModel.videoPlay=videoPlay
        Log.d(TAG,"TasksCenterScreen onLifeCreate")
    }
    Scaffold(
        topBar ={
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement= Arrangement.Center,
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
                        checked =(tasksCenterViewModel.show_flag.value==0),
                        onCheckedChange = {
                            if(it){
                                tasksCenterViewModel.show_flag.value=0
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = LocalContext.current.resources.getString(R.string.waiting_for_execution))
                    Checkbox(
                        checked =(tasksCenterViewModel.show_flag.value==1),
                        onCheckedChange = {
                            if(it){
                                tasksCenterViewModel.show_flag.value=1
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
                        checked =(tasksCenterViewModel.show_flag.value==2),
                        onCheckedChange = {
                            if(it){
                                tasksCenterViewModel.show_flag.value=2
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = LocalContext.current.resources.getString(R.string.execution_failed))
                    Spacer(modifier = Modifier.width(10.dp))
                    Checkbox(
                        checked =(tasksCenterViewModel.show_flag.value==3),
                        onCheckedChange = {
                            if(it){
                                tasksCenterViewModel.show_flag.value=3
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
                        checked =(tasksCenterViewModel.show_flag.value==4),
                        onCheckedChange = {
                            if(it){
                                tasksCenterViewModel.show_flag.value=4
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
            }
        })
    {
        paddingValues->ShowList(paddingValues, tasksCenterViewModel)
    }
    LaunchedEffect(true){
         var i=0
         while(true){
             if(activity.tasksBinder.getRemainingTasksNum()==0){
                 i++
             }
             else{
                 i=0
             }
             if(i>10){
                 break
             }
             tasksCenterViewModel.reFresh()
             delay(1000)
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
                    TextButton(onClick = {
                        tasksCenterViewModel.cancelTask(id)
                    }) {
                        Text(text=LocalContext.current.resources.getString(R.string.cancel))
                    }
//                    Text(text=tasksCenterViewModel.tasksState[id]!!.value)
                    showRunningTaskState(tasksCenterViewModel, id)
                }
            }
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
                    Icon(painter = painterResource(id = R.drawable.play_circle_24px),
                        tint = Color.Black,
                        modifier = Modifier
                            .width(24.dp)
                            .height(24.dp)
                            .background(color = Color.Transparent)
                            .clickable {
                        val file=File(path)
                        tasksCenterViewModel.videoPlay(file,VideoPlay.route)
                            }
                        ,
                        contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = LocalContext.current.resources.getString(R.string.ended))
                }
            }
        }
    }
}