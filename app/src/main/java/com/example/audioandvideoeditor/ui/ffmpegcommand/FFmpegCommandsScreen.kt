package com.example.audioandvideoeditor.ui.ffmpegcommand

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.application.AppApplication
import com.example.audioandvideoeditor.entity.TaskInfo
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.navigation.Destination
import com.example.audioandvideoeditor.utils.ConfigsUtils
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FFmpegCommandsScreen(
    nextDestination:()->Unit,
    goDestination:(route:String)->Unit,
    setNextToNextDestination:(route:String)->Unit,
    file:File?,
    ffmpegCommandsViewModel: FFmpegCommandsViewModel= viewModel()
){
    val life= rememberLifecycle()
    life.onLifeCreate {
        ffmpegCommandsViewModel.command_args_str.value=""
        ffmpegCommandsViewModel.command_args.clear()
        ffmpegCommandsViewModel.command_args.add(mutableStateOf(""))
        if(ffmpegCommandsViewModel.task_flag==0) {
            ffmpegCommandsViewModel.show_log_flag.value = false
            ffmpegCommandsViewModel.task_log_path = ""
            ffmpegCommandsViewModel.log_lines.clear()
        }
    }
    val assisted_edit=stringResource(id = R.string.assisted_edit)
    val self_edit=stringResource(id = R.string.self_edit)
    val titles = remember { listOf(assisted_edit,self_edit) }
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
                .weight(1f) // 让内容占据剩余空间
        ) { page ->
            if(page==0){
                SimpleCommandsEditor(
                    nextDestination,
                    goDestination,
                    setNextToNextDestination,
                    file,
                    ffmpegCommandsViewModel
                )
            }
            else{
                CommandsEditor(nextDestination, ffmpegCommandsViewModel)
            }
        }
    }

}

@Composable
private fun CommandsEditor(
    nextDestination:()->Unit,
    ffmpegCommandsViewModel: FFmpegCommandsViewModel
){
    val ctx= LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
    ) {

        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .height((LocalConfiguration.current.screenHeightDp - 250).dp),
            value = ffmpegCommandsViewModel.command_args_str.value,
            onValueChange = {
                ffmpegCommandsViewModel.command_args_str.value = it
            },
            label = { Text(LocalContext.current.getString(R.string.edit_command_line)) }
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                startSelfFFmpegCommandsTask(
                    ffmpegCommandsViewModel,
                    ctx,
                    nextDestination
                )
            }) {
                Text(text=LocalContext.current.getString(R.string.ok))
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}
@Composable
private fun SimpleCommandsEditor(
    nextDestination:()->Unit,
    goDestination:(route:String)->Unit,
    setNextToNextDestination:(route:String)->Unit,
    file:File?,
    viewModel: FFmpegCommandsViewModel
){
    val life= rememberLifecycle()
    val context= LocalContext.current
    life.onLifeCreate {
        if (file != null) {
            viewModel.input_file=file.path
        }
        if(viewModel.output_file_name.length==0)
        {
            val date = Date(System.currentTimeMillis())
            val formatter = SimpleDateFormat(
                "yyyyMMddHHmmss",
                context.resources.configuration.locales[0]
            )
            viewModel.output_file_name = formatter.format(date)
        }
        viewModel.parameterTemplateName= listOf(
            context.getString(R.string.video_encoding),
            context.getString(R.string.audio_encoding),
            context.getString(R.string.reset_resolution),
            context.getString(R.string.reset_video_bitrate),
            context.getString(R.string.reset_audio_bitrate),
            context.getString(R.string.reset_frame_rate),
            context.getString(R.string.reset_sampling_rate),
            context.getString(R.string.duration_cutting),
            context.getString(R.string.screen_clipping),
//            context.getString(R.string.reset_video_scale),
            context.getString(R.string.extract_audio),
            context.getString(R.string.video_muting),
        )
        viewModel.parameterNameText=context.getString(R.string.video_encoding)
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding=PaddingValues(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row {
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = stringResource(id = R.string.input_file) + ":")
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment=Alignment.CenterVertically
            ) {
                val width= LocalConfiguration.current.screenWidthDp
                OutlinedTextField(
                    value = viewModel.input_file,
                    onValueChange = {}, // 允许手动输入
                    readOnly = true, // 设置为 false 允许用户手动编辑
                    label = {},
                    singleLine =false,
                    modifier = Modifier
                        .width(width.dp*2/3)
                )
                Spacer(modifier = Modifier.width(20.dp))
                IconButton(onClick = {
                    goDestination(Destination.FileSelection.route)
                    setNextToNextDestination(Destination.FFmpegCommands.route)
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_folder_24)
                        , contentDescription = null
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row {
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = stringResource(id = R.string.output_file) + ":")
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment=Alignment.CenterVertically
            ){
                val width= LocalConfiguration.current.screenWidthDp
                OutlinedTextField(
                    value = viewModel.output_file_name,
                    onValueChange = {viewModel.output_file_name=it}, // 允许手动输入
                    readOnly = false, // 设置为 false 允许用户手动编辑
                    label = { },
                    singleLine =false,
                    modifier = Modifier
                        .width(width.dp*2/3)
                )
                Spacer(modifier = Modifier.width(10.dp))
                ExtensionTemplate(
                    viewModel,
                    modifier = Modifier.width(120.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ){
                Text(stringResource(id = R.string.parameter_template)+":")
                Spacer(modifier = Modifier.width(10.dp))
                ParameterTemplate(
                    viewModel,
                    modifier = Modifier.width(160.dp)
                )
            }
            OutlinedTextField(
                value = viewModel.parameterContextText,
                onValueChange = {
                    viewModel.parameterContextText=it
                }, // 允许手动输入
                readOnly = false, // 设置为 false 允许用户手动编辑
                label = {
                    Text(stringResource(id = R.string.parameter))
                },
                singleLine =false,
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                ,
                horizontalArrangement = Arrangement.Center
            ){
                val ctx= LocalContext.current
                Button(
                    onClick = {
                        startAssistedFFmpegCommandsTask(viewModel, ctx,nextDestination)
                    }
                ) {
                    Text(stringResource(id = R.string.ok))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtensionTemplate(
    viewModel: FFmpegCommandsViewModel,
    modifier: Modifier
){
    // 下拉菜单是否展开的状态
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = viewModel.extensionText,
            onValueChange = {viewModel.extensionText=it },
            readOnly = false,
            label = { },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor() // 必须有这个修饰符，才能将文本框作为菜单的锚点
        )
        // 下拉菜单
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            viewModel.extensionTemplate .forEach { template ->
                DropdownMenuItem(
                    text = { Text(template) },
                    onClick = {
                        viewModel.extensionText = template
                        expanded = false // 选中后收起菜单
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParameterTemplate(
    viewModel: FFmpegCommandsViewModel,
    modifier: Modifier
){
    // 下拉菜单是否展开的状态
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = viewModel.parameterNameText,
            onValueChange = { },
            readOnly = true,
            label = { },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor() // 必须有这个修饰符，才能将文本框作为菜单的锚点
        )
        // 下拉菜单
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            viewModel.parameterTemplateName .forEach { template ->
                DropdownMenuItem(
                    text = { Text(template) },
                    onClick = {
                        viewModel.parameterNameText = template
                        expanded = false // 选中后收起菜单
                        viewModel.parameterContextText=viewModel.parameterTemplateContext[
                            viewModel.parameterTemplateName.indexOf(template)
                        ]
                    }
                )
            }
        }
    }
}


private  fun startSelfFFmpegCommandsTask(
    ffmpegCommandsViewModel: FFmpegCommandsViewModel,
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
    AppApplication.INSTANCE.taskRepository.startNewTask(info)
    nextDestination()
}
private  fun startAssistedFFmpegCommandsTask(
    viewModel: FFmpegCommandsViewModel,
    ctx:Context,
    nextDestination:()->Unit,
){
    val cmd_str="ffmpeg -i input_file ${viewModel.parameterContextText} output_file"
    val command_arg_list=
        cmd_str
            .trim()
            .split("[\\s\\n]+".toRegex())
            .map {
                when(it){
                    "input_file"->viewModel.input_file
                    "output_file"->"${ConfigsUtils.target_dir}/${viewModel.output_file_name}.${viewModel.extensionText}"
                    else -> it
                }
            }
    val int_arr=ArrayList<Int>()
    int_arr.add(2)
    int_arr.add(command_arg_list.size)
    val long_arr=ArrayList<Long>()
    val str_arr=ArrayList<String>()
    val date= Date(System.currentTimeMillis())
    val formatter= SimpleDateFormat("yyyyMMddHHmmss", ctx.resources.configuration.locales[0])
    val task_log_path= ConfigsUtils.target_dir+"/"+ctx.getString(R.string.ffmpeg_command_line)+formatter.format(date)+".log"
    viewModel.task_log_path=task_log_path
    str_arr.add(task_log_path)
    str_arr.addAll(command_arg_list)
    val float_arr=ArrayList<Float>()
    val info= TaskInfo(
        int_arr,
        long_arr,
        str_arr,
        float_arr
    )
    AppApplication.INSTANCE.taskRepository.startNewTask(info)
    viewModel.input_file=""
    viewModel.output_file_name=""
    nextDestination()
}

