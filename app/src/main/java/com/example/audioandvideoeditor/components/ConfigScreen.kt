package com.example.audioandvideoeditor.components

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audioandvideoeditor.MainActivity
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.utils.FilesUtils
import com.example.audioandvideoeditor.viewmodel.ConfigViewModel

@Composable
fun ConfigScreen(
    activity: MainActivity,
    nextDestination: (route: String) -> Unit,
    configViewModel: ConfigViewModel= viewModel()
){
    val life= rememberLifecycle()
    life.onLifeCreate {
        configViewModel.initConfig(activity)
    }
    ConfigScreen2_2(activity,nextDestination, configViewModel)
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
//                    Text(text= LocalContext.current.getString(R.string.tips_for_switching_languages),
//                    fontSize = 15.sp
//                    )
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
                    Spacer(modifier = Modifier.height(20.dp))
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
                    Text(LocalContext.current.getString(R.string.ok))
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
            }
        )
    }
}


@Composable
private fun ConfigScreen2_2(
    activity: MainActivity,
    nextDestination: (route: String) -> Unit,
    configViewModel: ConfigViewModel
){
    val scrollState = rememberScrollState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement= Arrangement.Center,
        modifier = Modifier
            .verticalScroll(scrollState) // 关键的滚动修饰符
            .fillMaxWidth()
    ) {
        val currentVersion = LocalContext.current .packageManager.getPackageInfo(LocalContext.current.packageName, 0).versionName
        Column (
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ){
            Spacer(modifier = Modifier.height(40.dp))
            Icon(
                painter = painterResource(id = R.drawable.movie_edit_24px)
                , contentDescription = null
                , modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                currentVersion
                , fontWeight = FontWeight.Bold
            )           
            Spacer(modifier = Modifier.height(40.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
        }
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    configViewModel.showUpdateDialogFlag.value = true
                }
        ){
            Spacer(modifier = Modifier.height(40.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(LocalContext.current.getString(R.string.check_for_updates)
                    , fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
        }
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    configViewModel.editLanguageFlag.value = true
                }
        ){
            Spacer(modifier = Modifier.height(40.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(LocalContext.current.getString(R.string.switch_language)
                       , fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                )
                {
                    Text(configViewModel.LanguageText.value)
                    Spacer(modifier = Modifier.width(10.dp))
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
        }
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    configViewModel.showPathDialog.value = true
                }
        ){
            Spacer(modifier = Modifier.height(40.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(LocalContext.current.resources.getString(R.string.target_dir)
                    , fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                )
                {
                    Text(configViewModel.downloadPath.value)
                    Spacer(modifier = Modifier.width(10.dp))
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
        }
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    nextDestination(PrivacyPolicy.route)
                }
        ){
            Spacer(modifier = Modifier.height(40.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(stringResource(id = R.string.privacy_policy),fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(40.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
        }
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    nextDestination(APPInfo.route)
                }
        ){
            Spacer(modifier = Modifier.height(40.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(stringResource(id = R.string.app_info),fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(40.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
        }
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    nextDestination(LogDisplay.route)
                }
        ){
            Spacer(modifier = Modifier.height(40.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(stringResource(id = R.string.error_and_crash_logs),fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(40.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
        }
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    nextDestination(ContactDeveloper.route)
                }
        ){
            Spacer(modifier = Modifier.height(40.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(stringResource(id = R.string.contact_developer),fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(40.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
        }
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    nextDestination(Permissions.route)
                }
        ){
            Spacer(modifier = Modifier.height(40.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(stringResource(id = R.string.permissions),fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(40.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
        }
        val context= LocalContext.current
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    configViewModel.showClearFFmpegLogFilesDialogFlag.value = true
                }
        ){
            Spacer(modifier = Modifier.height(40.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(stringResource(id = R.string.clear_task_logs),fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(40.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
        }
    }
    showEditLanguageScreen(activity, configViewModel)
    showEditPathScreen(activity, configViewModel)
    ClearFFmpegLogFilesDialog(configViewModel)
    UpdateDialog(configViewModel)
    configViewModel.errorMessage.value?.let { ErrorDialog(it) { configViewModel.clearErrorMessage() } }
}
@Composable
fun ClearFFmpegLogFilesDialog(
    configViewModel: ConfigViewModel
){
    val context= LocalContext.current
    if(configViewModel.showClearFFmpegLogFilesDialogFlag.value) {
        AlertDialog(
            onDismissRequest = { configViewModel.showClearFFmpegLogFilesDialogFlag.value = false },
            title = {
                Text(text = LocalContext.current.getString(R.string.clear_task_logs))
            },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ){
                    Text(
                        text= stringResource(id = R.string.clear_tip2),
                        fontWeight = FontWeight.Bold
                    )
                }

            },
            confirmButton = {
                TextButton(
                    onClick = {
                        configViewModel.showClearFFmpegLogFilesDialogFlag.value = false
                        clearFFmpegLogFiles(context)
                        val toast = Toast.makeText(
                            context,
                            context.getString(R.string.clear_tip),
                            Toast.LENGTH_SHORT
                        );
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show()
                    }
                ) {
                    Text(LocalContext.current.getString(R.string.ok))
                }

            },
            dismissButton = {
                TextButton(
                    onClick = {
                        configViewModel.showClearFFmpegLogFilesDialogFlag.value = false
                    }
                ) {
                    Text(LocalContext.current.getString(R.string.cancel))
                }
            }
        )
    }
}


@Composable
private fun UpdateDialog(
    configViewModel: ConfigViewModel
){
    val context= LocalContext.current
    val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
    if(configViewModel.showUpdateDialogFlag.value) {
        if(
            ConfigsUtils.gitHubRelease!=null
            &&ConfigsUtils.isNewVersionAvailable(currentVersion, ConfigsUtils.gitHubRelease!!.tagName)
        ) {
            AlertDialog(
                onDismissRequest = { configViewModel.showUpdateDialogFlag.value = false },
                title = {
                    Text(
                        text = stringResource(id = R.string.updates_tip),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.updates_tip2)
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Spacer(modifier = Modifier.height(20.dp))
                            SelectionContainer {
                                Text(
                                    text = stringResource(id = R.string.releases_link),
                                    textDecoration = TextDecoration.Underline,
                                    color = Color.Blue,
                                    modifier = Modifier.clickable {
                                        FilesUtils.openWebLink(
                                            context,
                                            context.getString(R.string.releases_link)
                                        )
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            SelectionContainer {
                                Text(
                                    text = stringResource(id = R.string.lanzout_link),
                                    textDecoration = TextDecoration.Underline,
                                    color = Color.Blue,
                                    modifier = Modifier.clickable {
                                        FilesUtils.openWebLink(
                                            context,
                                            context.getString(R.string.lanzout_link)
                                        )
                                    }
                                )
                            }
                        }

                    }

                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            configViewModel.showUpdateDialogFlag.value = false

                        }
                    ) {
                        Text(LocalContext.current.getString(R.string.ok))
                    }

                },
                dismissButton = {
                }
            )
        }
        else{
            AlertDialog(
                onDismissRequest = { configViewModel.showUpdateDialogFlag.value = false },
                title = {
                    Text(
                        text = stringResource(id = R.string.updates_tip3),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            configViewModel.showUpdateDialogFlag.value = false

                        }
                    ) {
                        Text(LocalContext.current.getString(R.string.ok))
                    }

                },
                dismissButton = {
                }
            )
        }
    }
}


@Composable
fun showEditPathScreen(
activity: MainActivity,
configViewModel: ConfigViewModel
){
    if (configViewModel.showPathDialog.value) {
        var newDownloadPath by remember { mutableStateOf(configViewModel.downloadPath.value) }
        AlertDialog(
            onDismissRequest = { configViewModel.showPathDialog.value = false },
            title = { Text(stringResource(id = R.string.target_dir)) },
            text = {
                OutlinedTextField(
                    value = newDownloadPath,
                    onValueChange = { newDownloadPath = it },
                    label = { Text(stringResource(id = R.string.target_dir)) }
                )
            },
            confirmButton = {
                Button(onClick = {
                    configViewModel.updateDownloadPath(newDownloadPath,activity)
                    if (configViewModel.errorMessage.value == null) {
                        configViewModel.showPathDialog.value = false
                    }
                }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                Button(onClick = { configViewModel.showPathDialog.value  = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
@Composable
private fun ErrorDialog(errorMessage: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 8.dp) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme .error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
            }
        }
    }
}


/**
 * 清空应用 filesDir 目录下符合特定模式的日志文件。
 *
 * @param context 应用上下文
 * @param prefix 日志文件名的前缀，例如 "ffmpeg"
 * @param suffix 日志文件名的后缀，例如 ".log"
 */
fun clearFFmpegLogFiles(context: Context, prefix: String = "ffmpeg", suffix: String = ".log") {
    val filesDir = context.filesDir // 获取应用的内部文件目录

    // 确保目录存在
    if (!filesDir.exists() || !filesDir.isDirectory) {
        return
    }

    // 定义文件名模式的正则表达式
    // 这里假设日期时间格式是 "yyyyMMddHHmmss"
    val pattern = Regex("$prefix\\d{14}$suffix") // \\d{14} 匹配14位数字

    // 遍历 filesDir 目录下的所有文件和子目录
    filesDir.listFiles()?.forEach { file ->
        if (file.isFile) { // 只处理文件，不处理子目录
            if (file.name.matches(pattern)) { // 如果文件名符合模式
                val deleted = file.delete() // 尝试删除文件
                if (deleted) {
                    //println("已删除日志文件: ${file.name}") // 可以在这里添加日志输出或Toast提示
                    // Log.d("LogCleaner", "已删除日志文件: ${file.name}")
                } else {
                    //println("未能删除日志文件: ${file.name}")
                    // Log.w("LogCleaner", "未能删除日志文件: ${file.name}")
                }
            }
        }
    }
}



@Composable
private fun ConfigScreen2(
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
//        Spacer(modifier = Modifier.height(10.dp))
//        Button(onClick = {
//            configViewModel.editSizeForMaxTasksNumFlag.value=true
//        }) {
//            Text(text="${LocalContext.current.resources.getString(R.string.size_for_max_tasks_num_text)}:${configViewModel.sizeForMaxTasksNumText.value}")
//        }
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
                    Text(LocalContext.current.resources.getString(R.string.ok))
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
                    Text(LocalContext.current.getString(R.string.ok))
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
                    Text(LocalContext.current.getString(R.string.ok))
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