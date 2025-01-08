package com.example.audioandvideoeditor.components

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audioandvideoeditor.MainActivity
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.entity.TaskInfo
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.viewmodel.FFmpegCommandsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun FFmpegCommandsScreen(
    activity: MainActivity,
    nextDestination:()->Unit,
    ffmpegCommandsViewModel: FFmpegCommandsViewModel= viewModel()
){
    val life= rememberLifecycle()
    life.onLifeCreate {
        ffmpegCommandsViewModel.command_args_str.value=""
        ffmpegCommandsViewModel.command_args.clear()
        ffmpegCommandsViewModel.command_args.add(mutableStateOf(""))
        if(ffmpegCommandsViewModel.task_flag==0) {
//            ffmpegCommandsViewModel.read_log_flag = 0
            ffmpegCommandsViewModel.show_log_flag.value = false
            ffmpegCommandsViewModel.task_log_path = ""
            ffmpegCommandsViewModel.log_lines.clear()
        }
    }
    CommandsEditor(activity, nextDestination, ffmpegCommandsViewModel)
    readLog(nextDestination,ffmpegCommandsViewModel)
}
@Composable
fun CommandsList(
    activity: MainActivity,
    nextDestination:()->Unit,
    ffmpegCommandsViewModel: FFmpegCommandsViewModel
){

    LazyColumn(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        contentPadding = PaddingValues(horizontal = 40.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
    ){
        item {
            Button(onClick = {
                ffmpegCommandsViewModel.command_args.add(mutableStateOf(""))
            }) {
               Text(text="添加一个编辑框")
            }
        }
        item {
            val ctx= LocalContext.current
            Button(onClick = {
                startFFmpegCommandsTask(
                    ffmpegCommandsViewModel,
                    activity,
                    ctx,
                    nextDestination
                )
            }) {
                Text(text="确定执行命令")
            }
        }
        items(
         count = ffmpegCommandsViewModel.command_args.size
      ){
            val index=it
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                value = ffmpegCommandsViewModel.command_args[index].value,
                onValueChange = {
                    ffmpegCommandsViewModel.command_args[index].value = it
                },
                label = { Text("编辑命令行") }
            )
      }
    }
}

@Composable
fun CommandsEditor(
    activity: MainActivity,
    nextDestination:()->Unit,
    ffmpegCommandsViewModel: FFmpegCommandsViewModel
){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        val ctx= LocalContext.current
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                if(ffmpegCommandsViewModel.task_flag==0){
                startFFmpegCommandsTask(
                    ffmpegCommandsViewModel,
                    activity,
                    ctx,
                    nextDestination
                )
                }
                else{
                    sendToast(ctx,"有FFmpeg命令正在执行，请稍后添加")
                }
            }) {
                Text(text=LocalContext.current.getString(R.string.determine))
            }
            Spacer(modifier = Modifier.width(20.dp))
            Button(onClick = {
                if(ffmpegCommandsViewModel.task_flag==1) {
                    ffmpegCommandsViewModel.show_log_flag.value = true
                }
                else{
                    sendToast(ctx,"没有正在执行的FFmpeg命令")
                }
            }) {
                Text(text=LocalContext.current.getString(R.string.log))
            }
        }
//        Button(onClick = {
//            startFFmpegCommandsTask(
//                ffmpegCommandsViewModel,
//                activity,
//                ctx,
//                nextDestination
//            )
//        }) {
//            Text(text=LocalContext.current.getString(R.string.determine))
//        }
        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            modifier = Modifier
                .fillMaxSize(),
            value = ffmpegCommandsViewModel.command_args_str.value,
            onValueChange = {
                ffmpegCommandsViewModel.command_args_str.value = it
            },
            label = { Text(LocalContext.current.getString(R.string.edit_command_line)) }
        )
    }
}
private  fun startFFmpegCommandsTask(
    ffmpegCommandsViewModel: FFmpegCommandsViewModel,
    activity: MainActivity,
    ctx:Context,
    nextDestination:()->Unit,
){
    val command_arg_list=ffmpegCommandsViewModel.command_args_str.value.trim().split("[\\s\\n]+".toRegex())
    val int_arr=ArrayList<Int>()
    int_arr.add(2)
    int_arr.add(command_arg_list.size)
    val long_arr=ArrayList<Long>()
    val str_arr=ArrayList<String>()
    val date= Date(System.currentTimeMillis())
    val formatter= SimpleDateFormat("yyyyMMddHHmmss", ctx.resources.configuration.locales[0])
    val task_log_path= ConfigsUtils.target_dir+"/"+ctx.getString(R.string.ffmpeg_command_line)+formatter.format(date)+".log"
    ffmpegCommandsViewModel.task_log_path=task_log_path
    str_arr.add(task_log_path)
    str_arr.addAll(command_arg_list)
    val float_arr=ArrayList<Float>()
    val info= TaskInfo(
        int_arr,
        long_arr,
        str_arr,
        float_arr
    )
    ffmpegCommandsViewModel.task_flag=1
    activity.tasksBinder.startTask(info)
//    ffmpegCommandsViewModel.command_args.clear()
    ffmpegCommandsViewModel.startReadLogFile()
    ffmpegCommandsViewModel.show_log_flag.value=true
//    nextDestination()
}
private  fun startFFmpegCommandsTask2(
    ffmpegCommandsViewModel: FFmpegCommandsViewModel,
    activity: MainActivity,
    ctx:Context,
    nextDestination:()->Unit,
){
    var command_args_str=""
    ffmpegCommandsViewModel.command_args.forEach {
        if(it.value.length!=0){
            command_args_str=command_args_str+" "+it.value
        }
    }
    val command_arg_list=command_args_str.trim().split("[\\s\\n]+".toRegex())
    val int_arr=ArrayList<Int>()
    int_arr.add(2)
    int_arr.add(command_arg_list.size)
    val long_arr=ArrayList<Long>()
    val str_arr=ArrayList<String>()
    val date= Date(System.currentTimeMillis())
    val formatter= SimpleDateFormat("yyyyMMddHHmmss", ctx.resources.configuration.locales[0])
    val task_log_name= ConfigsUtils.target_dir+"/"+"FFmpeg命令行"+formatter.format(date)+".log"
    str_arr.add(task_log_name)
    str_arr.addAll(command_arg_list)
    val float_arr=ArrayList<Float>()
    val info= TaskInfo(
        int_arr,
        long_arr,
        str_arr,
        float_arr
    )
    activity.tasksBinder.startTask(info)
    ffmpegCommandsViewModel.command_args.clear()
    nextDestination()
}

@Composable
private fun readLog(
    nextDestination:()->Unit,
    ffmpegCommandsViewModel: FFmpegCommandsViewModel
){
if(ffmpegCommandsViewModel.show_log_flag.value){
        AlertDialog(
            onDismissRequest ={
                ffmpegCommandsViewModel.show_log_flag.value=false
            },
            title ={Text(text=LocalContext.current.getString(R.string.log))},
            text ={
                val scrollState = rememberLazyListState()
                LazyColumn(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    modifier = Modifier
                        .fillMaxSize(),
                    state=scrollState
                ){
                    items(
                        count = ffmpegCommandsViewModel.log_lines.size
                    ){
                        Text(text = ffmpegCommandsViewModel.log_lines[it])
                    }
                }
//                LaunchedEffect(true){
//                    while(ffmpegCommandsViewModel.read_log_flag!=2&&this.isActive){
//                        if(ffmpegCommandsViewModel.task_log_path.length!=0&&ffmpegCommandsViewModel.read_log_flag==0) {
//                            val file=File(ffmpegCommandsViewModel.task_log_path)
//                            if(file.exists()&&file.canRead()){
//                                val reader = FileReader(file)
//                                val bufferedReader = BufferedReader(reader)
//                                ffmpegCommandsViewModel.file = file
//                                ffmpegCommandsViewModel.bufferedReader = bufferedReader
//                                ffmpegCommandsViewModel.read_log_flag=1
//                            }
//                            else{
//                                delay(100)
//                                continue
//                            }
//                        }else{
//                            ffmpegCommandsViewModel.readLogFile()
//                            delay(100)
//                        }
//                    }
//                }
                },
            confirmButton ={
                  TextButton(onClick = {
                      //nextDestination()
                      ffmpegCommandsViewModel.show_log_flag.value=false
                  }) {
                      Text(LocalContext.current.getString(R.string.determine))
                  }
            },
            dismissButton ={

            },
        )
}
}

private  fun sendToast(ctx:Context,text:String){
    val toast = Toast.makeText( ctx, text, Toast.LENGTH_SHORT)
    toast.setGravity(Gravity.CENTER, 0, 0)
    toast.show()
}