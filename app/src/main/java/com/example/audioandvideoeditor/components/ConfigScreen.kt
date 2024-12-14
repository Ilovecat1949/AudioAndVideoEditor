package com.example.audioandvideoeditor.components

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audioandvideoeditor.MainActivity
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.viewmodel.ConfigViewModel

@Composable
fun ConfigScreen(
    activity: MainActivity,
    configViewModel: ConfigViewModel= viewModel()
){
    val life= rememberLifecycle()
    life.onLifeCreate {
        configViewModel.initConfig(activity)
    }
    ConfigScreen2(activity, configViewModel)
}
@Composable
fun ConfigScreen2(
    activity: MainActivity,
    configViewModel: ConfigViewModel
){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement= Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = {
            configViewModel.editSizeForVideoEncodingTaskFlag.value=true
        }) {
            Text(text="${LocalContext.current.resources.getString(R.string.size_for_video_encoding_task_text)}:${configViewModel.sizeForVideoEncodingTaskText.value}")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = {
            configViewModel.editSizeForAudioEncodingTaskFlag.value=true
        }) {
            Text(text="${LocalContext.current.resources.getString(R.string.size_for_audio_encoding_task_text)}:${configViewModel.sizeForAudioEncodingTaskText.value}")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = {
            configViewModel.editSizeForMaxTasksNumFlag.value=true
        }) {
            Text(text="${LocalContext.current.resources.getString(R.string.size_for_max_tasks_num_text)}:${configViewModel.sizeForMaxTasksNumText.value}")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = {
            configViewModel.editLanguageFlag.value=true
        }) {
            Text(text="${LocalContext.current.getString(R.string.switch_language)}:${configViewModel.LanguageText .value}")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text="${LocalContext.current.resources.getString(R.string.target_dir)}:${ConfigsUtils.target_dir}")
    }
    showEditSizeForVideoEncodingTaskScreen(activity,configViewModel)
    showEditSizeForAudioEncodingTaskScreen(activity,configViewModel)
    showEditSizeForMaxTasksNumScreen(activity,configViewModel)
    showEditLanguageScreen(activity, configViewModel)
}
@Composable
private fun showEditSizeForVideoEncodingTaskScreen(
    activity: MainActivity,
    configViewModel: ConfigViewModel
){
    if(configViewModel.editSizeForVideoEncodingTaskFlag.value) {
        AlertDialog(
            onDismissRequest = { configViewModel.editSizeForVideoEncodingTaskFlag.value = false },
            title = {
                Text(text = LocalContext.current.resources.getString(R.string.set_cache_size))
            },
            text = {
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
                            .fillMaxWidth()){
                        Text(text = "10MB")
                        Checkbox(
                            checked =(configViewModel.checkSizeForVideoEncodingTaskFlag.value==0),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkSizeForVideoEncodingTaskFlag.value=0
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()){
                        Text(text = "50MB")
                        Checkbox(
                            checked =(configViewModel.checkSizeForVideoEncodingTaskFlag.value==1),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkSizeForVideoEncodingTaskFlag.value=1
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()){
                        Text(text = "100MB")
                        Checkbox(
                            checked =(configViewModel.checkSizeForVideoEncodingTaskFlag.value==2),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkSizeForVideoEncodingTaskFlag.value=2
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()){
                        Text(text = "500MB")
                        Checkbox(
                            checked =(configViewModel.checkSizeForVideoEncodingTaskFlag.value==3),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkSizeForVideoEncodingTaskFlag.value=3
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()){
                        Text(text = "1GB")
                        Checkbox(
                            checked =(configViewModel.checkSizeForVideoEncodingTaskFlag.value==4),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkSizeForVideoEncodingTaskFlag.value=4
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()){
                        Text(text = "2GB")
                        Checkbox(
                            checked =(configViewModel.checkSizeForVideoEncodingTaskFlag.value==5),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkSizeForVideoEncodingTaskFlag.value=5
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()){
                        Text(text = "3GB")
                        Checkbox(
                            checked =(configViewModel.checkSizeForVideoEncodingTaskFlag.value==6),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkSizeForVideoEncodingTaskFlag.value=6
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        configViewModel.setSizeForVideoEncodingTask(activity)
                        configViewModel.editSizeForVideoEncodingTaskFlag.value = false
                    }
                ) {
                    Text(LocalContext.current.resources.getString(R.string.determine))
                }

            },
            dismissButton = {
                TextButton(
                    onClick = {
                        configViewModel.editSizeForVideoEncodingTaskFlag.value = false
                    }
                ) {
                    Text(LocalContext.current.resources.getString(R.string.cancel))
                }
            })
    }
}

@Composable
private fun showEditSizeForAudioEncodingTaskScreen(
    activity: MainActivity,
    configViewModel: ConfigViewModel
){
    if(configViewModel.editSizeForAudioEncodingTaskFlag.value) {
        AlertDialog(
            onDismissRequest = { configViewModel.editSizeForAudioEncodingTaskFlag.value = false },
            title = {
                Text(text =LocalContext.current.resources.getString(R.string.set_cache_size))
            },
            text = {
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
                            .fillMaxWidth()){
                        Text(text = "10MB")
                        Checkbox(
                            checked =(configViewModel.checkSizeForAudioEncodingTaskFlag.value==0),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkSizeForAudioEncodingTaskFlag.value=0
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()){
                        Text(text = "50MB")
                        Checkbox(
                            checked =(configViewModel.checkSizeForAudioEncodingTaskFlag.value==1),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkSizeForAudioEncodingTaskFlag.value=1
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()){
                        Text(text = "100MB")
                        Checkbox(
                            checked =(configViewModel.checkSizeForAudioEncodingTaskFlag.value==2),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkSizeForAudioEncodingTaskFlag.value=2
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()){
                        Text(text = "500MB")
                        Checkbox(
                            checked =(configViewModel.checkSizeForAudioEncodingTaskFlag.value==3),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkSizeForAudioEncodingTaskFlag.value=3
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()){
                        Text(text = "1GB")
                        Checkbox(
                            checked =(configViewModel.checkSizeForAudioEncodingTaskFlag.value==4),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkSizeForAudioEncodingTaskFlag.value=4
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()){
                        Text(text = "2GB")
                        Checkbox(
                            checked =(configViewModel.checkSizeForAudioEncodingTaskFlag.value==5),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkSizeForAudioEncodingTaskFlag.value=5
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()){
                        Text(text = "3GB")
                        Checkbox(
                            checked =(configViewModel.checkSizeForAudioEncodingTaskFlag.value==6),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkSizeForAudioEncodingTaskFlag.value=6
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        configViewModel.setSizeForAudioEncodingTask(activity)
                        configViewModel.editSizeForAudioEncodingTaskFlag.value = false
                    }
                ) {
                    Text(LocalContext.current.getString(R.string.determine))
                }

            },
            dismissButton = {
                TextButton(
                    onClick = {
                        configViewModel.editSizeForAudioEncodingTaskFlag.value = false
                    }
                ) {
                    Text(LocalContext.current.getString(R.string.cancel))
                }
            })
    }
}

@Composable
private fun showEditSizeForMaxTasksNumScreen(
    activity: MainActivity,
    configViewModel: ConfigViewModel
){
    if(configViewModel.editSizeForMaxTasksNumFlag.value) {
        AlertDialog(
            onDismissRequest = { configViewModel.editSizeForMaxTasksNumFlag.value = false },
            title = {
                Text(text = LocalContext.current.resources.getString(R.string.size_for_max_tasks_num_text))
            },
            text = {
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
                            .fillMaxWidth()){
                        Text(text = "2")
                        Checkbox(
                            checked =(configViewModel.checkSizeForMaxTasksNumFlag.value==0),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkSizeForMaxTasksNumFlag.value=0
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()){
                        Text(text = "3")
                        Checkbox(
                            checked =(configViewModel.checkSizeForMaxTasksNumFlag.value==1),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkSizeForMaxTasksNumFlag.value=1
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()){
                        Text(text = "4")
                        Checkbox(
                            checked =(configViewModel.checkSizeForMaxTasksNumFlag.value==2),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkSizeForMaxTasksNumFlag.value=2
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()){
                        Text(text = "5")
                        Checkbox(
                            checked =(configViewModel.checkSizeForMaxTasksNumFlag.value==3),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkSizeForMaxTasksNumFlag.value=3
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()){
                        Text(text = "6")
                        Checkbox(
                            checked =(configViewModel.checkSizeForMaxTasksNumFlag.value==4),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkSizeForMaxTasksNumFlag.value=4
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()){
                        Text(text = "7")
                        Checkbox(
                            checked =(configViewModel.checkSizeForMaxTasksNumFlag.value==5),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkSizeForMaxTasksNumFlag.value=5
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()){
                        Text(text = "8")
                        Checkbox(
                            checked =(configViewModel.checkSizeForMaxTasksNumFlag.value==6),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkSizeForMaxTasksNumFlag.value=6
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        configViewModel.setMaxTasksNum(activity)
                        configViewModel.editSizeForMaxTasksNumFlag.value = false
                    }
                ) {
                    Text(LocalContext.current.getString(R.string.determine))
                }

            },
            dismissButton = {
                TextButton(
                    onClick = {
                        configViewModel.editSizeForMaxTasksNumFlag.value = false
                    }
                ) {
                    Text(LocalContext.current.getString(R.string.cancel))
                }
            })
    }
}

@Composable
private fun showEditLanguageScreen(
    activity: MainActivity,
    configViewModel: ConfigViewModel
){
    if(configViewModel.editLanguageFlag.value) {
        AlertDialog(
            onDismissRequest = { configViewModel.editLanguageFlag.value = false },
            title = {
                Text(text = LocalContext.current.getString(R.string.switch_language))
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement= Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text= LocalContext.current.getString(R.string.tips_for_switching_languages),
                    fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()){
                        Text(text = LocalContext.current.getString(R.string.simplified_chinese))
                        Checkbox(
                            checked =(configViewModel.checkLanguageFlag.value==0),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkLanguageFlag.value=0
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()){
                        Text(text = LocalContext.current.getString(R.string.english))
                        Checkbox(
                            checked =(configViewModel.checkLanguageFlag.value==1),
                            onCheckedChange = {
                                if(it){
                                    configViewModel.checkLanguageFlag.value=1
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        configViewModel.setLanguage(activity)
                        configViewModel.editLanguageFlag.value = false
                    }
                ) {
                    Text(LocalContext.current.getString(R.string.determine))
                }

            },
            dismissButton = {
                TextButton(
                    onClick = {
                        configViewModel.editLanguageFlag.value = false
                    }
                ) {
                    Text(LocalContext.current.getString(R.string.cancel))
                }
            })
    }
}