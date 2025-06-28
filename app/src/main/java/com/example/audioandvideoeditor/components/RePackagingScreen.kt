package com.example.audioandvideoeditor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
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
import com.example.audioandvideoeditor.viewmodel.RePackagingViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun RePackagingScreen(
    activity: MainActivity,
    file: File,
    nextDestination:()->Unit,
    rePackagingViewModel: RePackagingViewModel= viewModel()
){
    val life= rememberLifecycle()
    val context= LocalContext.current
    life.onLifeCreate {
        if(rePackagingViewModel.input_file_name.value.length==0){
            val date= Date(System.currentTimeMillis())
            val formatter= SimpleDateFormat("yyyyMMddHHmmss", context.resources.configuration.locales[0])
            rePackagingViewModel.input_file_name.value="重封装"+formatter.format(date)
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement= Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        VideosPlayScreen(modifier = Modifier
            .height(200.dp)
            .fillMaxWidth(),
            path =file.path
        )
        RePackagingScreen2(activity, file, nextDestination, rePackagingViewModel)
    }
}

@Composable
fun RePackagingScreen2(
    activity: MainActivity,
    file: File,
    nextDestination:()->Unit,
    rePackagingViewModel: RePackagingViewModel
){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement= Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(text=file.name)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            TextField(
                modifier = Modifier.width(250.dp),
                value = rePackagingViewModel.input_file_name.value,
                onValueChange = {
                    rePackagingViewModel.input_file_name.value = it
                },
                label = { Text(LocalContext.current.resources.getString(R.string.file_name)) }
            )
            Spacer(modifier = Modifier.width(5.dp))
            TypesMenu(
                modifier =Modifier.width(120.dp),
                rePackagingViewModel = rePackagingViewModel
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = {
            startRePackaging(activity, file, nextDestination, rePackagingViewModel)
        }) {
            Text(text = "重封装")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(60.dp)
                .fillMaxWidth()
        ) {
            TextField(
                modifier = Modifier.width(250.dp),
                value = rePackagingViewModel.start_time_text.value,
                onValueChange = {
                    rePackagingViewModel.start_time_text.value = it
                },
                label = { Text("开始时间") }
            )
            Spacer(modifier = Modifier.width(10.dp))
            Button(onClick = {
                rePackagingViewModel.start_time=rePackagingViewModel.start_time_text.value.toLong()
            }) {
                Text(text = "确定")
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(60.dp)
                .fillMaxWidth()
        ) {
            TextField(
                modifier = Modifier.width(250.dp),
                value = rePackagingViewModel.end_time_text.value,
                onValueChange = {
                    rePackagingViewModel.end_time_text.value = it
                },
                label = { Text("结束时间") }
            )
            Spacer(modifier = Modifier.width(10.dp))
            Button(onClick = {
                rePackagingViewModel.end_time=rePackagingViewModel.end_time_text.value.toLong()
            }) {
                Text(text = "确定")
            }
        }
    }
}

private  fun startRePackaging(
    activity: MainActivity,
    file:File,
    nextDestination:()->Unit,
    rePackagingViewModel: RePackagingViewModel
){
    val int_arr=ArrayList<Int>()
    int_arr.add(1)
    val long_arr=ArrayList<Long>()
    long_arr.add(rePackagingViewModel.start_time)
    long_arr.add(rePackagingViewModel.end_time)
    val str_arr=ArrayList<String>()
    val targetPath= ConfigsUtils.target_dir+"/"+rePackagingViewModel.input_file_name.value+"."+rePackagingViewModel.selectedOptionText.value
    str_arr.add(targetPath)
    str_arr.add(file.path)
    val float_arr=ArrayList<Float>()
    val info= TaskInfo(
        int_arr,
        long_arr,
        str_arr,
        float_arr
    )
    activity.tasksBinder.startTask(info)
    rePackagingViewModel.input_file_name.value=""
    rePackagingViewModel.start_time_text.value=""
    rePackagingViewModel.end_time_text.value=""
    rePackagingViewModel.start_time=-1L
    rePackagingViewModel.end_time=-1L
    nextDestination()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypesMenu(
    modifier: Modifier,
    rePackagingViewModel: RePackagingViewModel
){
    val options = rePackagingViewModel.options
    val expanded =rePackagingViewModel.expanded
    val selectedOptionText =rePackagingViewModel.selectedOptionText
    // We want to react on tap/press on TextField to show menu
    ExposedDropdownMenuBox(
        modifier=modifier,
        expanded = expanded.value,
        onExpandedChange = {
            expanded.value = !expanded.value
        }
    ) {
        TextField(
            readOnly = true,
            value = selectedOptionText.value,
            onValueChange = { },
            label = { Text(LocalContext.current.getString(R.string.format)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded.value
                )
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            modifier = Modifier.menuAnchor()
        )

        ExposedDropdownMenu(
//            modifier=modifier,
            expanded = expanded.value,
            onDismissRequest = {
                expanded.value = false
            },
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = {
                        Text(selectionOption)
                    },
                    onClick = {
                        selectedOptionText.value = selectionOption
                        expanded.value = false
                    }
                )
            }
        }
    }
}