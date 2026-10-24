package com.example.audioandvideoeditor.ui.ffmpegcommand

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
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
    nextDestination: () -> Unit,
    goDestination: (route: String) -> Unit,
    setNextToNextDestination: (route: String) -> Unit,
    file_list: List<File>?,
    ffmpegCommandsViewModel: FFmpegCommandsViewModel = viewModel()
) {
    val life = rememberLifecycle()
    life.onLifeCreate {
        ffmpegCommandsViewModel.command_args_str.value = ""
        ffmpegCommandsViewModel.command_args.clear()
        ffmpegCommandsViewModel.command_args.add(mutableStateOf(""))
        if (ffmpegCommandsViewModel.task_flag == 0) {
            ffmpegCommandsViewModel.show_log_flag.value = false
            ffmpegCommandsViewModel.task_log_path = ""
            ffmpegCommandsViewModel.log_lines.clear()
        }
    }
    val assisted_edit = stringResource(id = R.string.assisted_edit)
    val self_edit = stringResource(id = R.string.self_edit)
    val titles = remember { listOf(assisted_edit, self_edit) }
    val pagerState = rememberPagerState(initialPage = 0) { titles.size }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
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
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            if (page == 0) {
                SimpleCommandsEditor(
                    nextDestination,
                    goDestination,
                    setNextToNextDestination,
                    file_list,
                    ffmpegCommandsViewModel
                )
            } else {
                CommandsEditor(nextDestination, ffmpegCommandsViewModel)
            }
        }
    }
}

@Composable
private fun CommandsEditor(
    nextDestination: () -> Unit,
    ffmpegCommandsViewModel: FFmpegCommandsViewModel
) {
    val ctx = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .height((LocalConfiguration.current.screenHeightDp - 250).dp),
            value = ffmpegCommandsViewModel.command_args_str.value,
            onValueChange = { ffmpegCommandsViewModel.command_args_str.value = it },
            label = { Text(LocalContext.current.getString(R.string.edit_command_line)) }
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                startSelfFFmpegCommandsTask(ffmpegCommandsViewModel, ctx, nextDestination)
            }) {
                Text(text = LocalContext.current.getString(R.string.ok))
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun SimpleCommandsEditor(
    nextDestination: () -> Unit,
    goDestination: (route: String) -> Unit,
    setNextToNextDestination: (route: String) -> Unit,
    file_list: List<File>?,
    viewModel: FFmpegCommandsViewModel
) {
    val life = rememberLifecycle()
    val context = LocalContext.current
    life.onLifeCreate {
        viewModel.updateInputFiles(file_list)

        if (viewModel.output_file_name.isEmpty()) {
            val date = Date(System.currentTimeMillis())
            val formatter = SimpleDateFormat(
                "yyyyMMddHHmmss",
                context.resources.configuration.locales[0]
            )
            viewModel.output_file_name = formatter.format(date)
        }

        val defaultNames = listOf(
            context.getString(R.string.video_encoding),
            context.getString(R.string.audio_encoding),
            context.getString(R.string.deinterlace),
            context.getString(R.string.reset_resolution),
            context.getString(R.string.reset_video_bitrate),
            context.getString(R.string.reset_audio_bitrate),
            context.getString(R.string.reset_frame_rate),
            context.getString(R.string.reset_sampling_rate),
            context.getString(R.string.duration_cutting),
            context.getString(R.string.screen_clipping),
            context.getString(R.string.extract_audio),
            context.getString(R.string.video_muting),
        )
        val defaultContexts = listOf(
            "-c:v libx264 -q:v 5 -c:a aac -ab 128k -ar 44100",
            "-c:v copy -c:a aac -ab 128k -ar 44100",
            "-deinterlace",
            "-vf scale=1280:720",
            "-b:v 500k",
            "-b:a 128k",
            "-r 45",
            "-ar 48000",
            "-ss 00:02:00 -t 00:03:00",
            "-vf crop=3/5*iw:ih:iw/5:0",
            "-vn -c:a copy",
            "-c:v copy -an",
        )
        viewModel.initTemplates(context, defaultNames, defaultContexts)
    }

    val selectedFiles = viewModel.selectedInputFiles

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp), // 稍作外边距扩充，增加呼吸感
        verticalArrangement = Arrangement.spacedBy(20.dp) // 统一的区块级节奏
    ) {
        item {
            // 🌟 1. 输入文件区：用 Surface 包装成精致的“卡片面板”
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 标题与选择图标行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedFiles.size > 1) stringResource(id = R.string.input_files_selected, selectedFiles.size)
                            else stringResource(id = R.string.input_file) + ":",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = {
                            goDestination(Destination.FileSelection2.route)
                            setNextToNextDestination(Destination.FFmpegCommands.route)
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_folder_24),
                                contentDescription = stringResource(R.string.file),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // 输入文件展示逻辑
                    if (selectedFiles.isEmpty()) {
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text(stringResource(R.string.select_file_hint)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (selectedFiles.size == 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = selectedFiles[0].name,
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearInputFiles() }) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(selectedFiles) { index, file ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${index + 1}. ${file.name}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.removeInputFile(file) },
                                            modifier = Modifier.height(24.dp).width(24.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                                                contentDescription = stringResource(R.string.delete),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🌟 2. 输出文件命名区
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (selectedFiles.size > 1) stringResource(R.string.output_file_prefix)
                    else stringResource(id = R.string.output_file) + ":",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val width = LocalConfiguration.current.screenWidthDp
                    OutlinedTextField(
                        value = viewModel.output_file_name,
                        onValueChange = { viewModel.output_file_name = it },
                        readOnly = false,
                        singleLine = true,
                        label = {
                            if (selectedFiles.size > 1) Text(stringResource(R.string.prefix_example_hint))
                        },
                        modifier = Modifier.width((width * 0.58).dp)
                    )
                    ExtensionTemplate(
                        viewModel,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🌟 3. 参数模板区
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.parameter_template) + ":",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                ParameterTemplate(
                    viewModel,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.parameterContextText,
                onValueChange = { viewModel.parameterContextText = it },
                readOnly = false,
                label = { Text(stringResource(id = R.string.parameter)) },
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 🌟 4. 底部主操作按钮（强化权重与视觉分量）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val ctx = LocalContext.current
                Button(
                    onClick = {
                        startAssistedFFmpegCommandsTask(viewModel, ctx, nextDestination)
                    },
                    enabled = selectedFiles.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp) // 药丸形现代商业级按钮
                ) {
                    Text(
                        text = if (selectedFiles.size > 1) stringResource(R.string.batch_execute_tasks, selectedFiles.size)
                        else stringResource(id = R.string.ok),
                        style = MaterialTheme.typography.bodyLarge
                    )
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
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = viewModel.extensionText,
            onValueChange = { viewModel.extensionText = it },
            readOnly = false,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            viewModel.extensionTemplate.forEach { template ->
                DropdownMenuItem(
                    text = { Text(template) },
                    onClick = {
                        viewModel.extensionText = template
                        expanded = false
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
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var dialogMode by remember { mutableStateOf<Any?>(null) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = viewModel.parameterNameText,
                onValueChange = { },
                readOnly = true,
                label = { Text(stringResource(id = R.string.parameter_template)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                val customList = viewModel.templates.filter { !it.isBuiltIn }
                val builtInList = viewModel.templates.filter { it.isBuiltIn }

                if (customList.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.custom_template_group), color = MaterialTheme.colorScheme.primary) },
                        onClick = { },
                        enabled = false // ✅ 设为不可点击，避免点击时出现高亮灰色背景
                    )
                    customList.forEach { template ->
                        DropdownMenuItem(
                            text = { Text("⚡ ${template.name}") },
                            onClick = {
                                viewModel.selectTemplate(template)
                                expanded = false
                            }
                        )
                    }
                }

                if (builtInList.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.builtin_template_group), color = MaterialTheme.colorScheme.secondary) },
                        onClick = { },
                        enabled = false // ✅ 设为不可点击
                    )
                    builtInList.forEach { template ->
                        DropdownMenuItem(
                            text = { Text(template.name) },
                            onClick = {
                                viewModel.selectTemplate(template)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        IconButton(onClick = { dialogMode = "ADD" }) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_add),
                contentDescription = stringResource(R.string.add_template),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        val currentSelected = viewModel.selectedTemplate
        val isCustom = currentSelected != null && !currentSelected.isBuiltIn
        // 2. 编辑按钮（通过 0.18f 的低透明度与激活态形成强烈反差）
        IconButton(
            onClick = { if (currentSelected != null) dialogMode = currentSelected },
            enabled = isCustom
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_edit),
                contentDescription = stringResource(R.string.edit_template),
                tint = if (isCustom) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f) // 显著降低不可用时的存在感
                }
            )
        }

// 3. 删除按钮（激活时显示警示红，禁用时同样深度置灰）
        IconButton(
            onClick = {
                if (currentSelected != null) {
                    viewModel.deleteCustomTemplate(context, currentSelected.id)
                }
            },
            enabled = isCustom
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_delete),
                contentDescription = stringResource(R.string.delete_template),
                tint = if (isCustom) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f) // 显著降低不可用时的存在感
                }
            )
        }
    }

    when (val mode = dialogMode) {
        "ADD" -> {
            TemplateEditDialog(
                title = stringResource(R.string.add_custom_template),
                initialName = "",
                initialContext = "",
                onDismiss = { dialogMode = null },
                onSave = { name, paramContext ->
                    viewModel.addCustomTemplate(context, name, paramContext)
                    dialogMode = null
                }
            )
        }
        is com.example.audioandvideoeditor.utils.FFmpegTemplate -> {
            TemplateEditDialog(
                title = stringResource(R.string.edit_custom_template),
                initialName = mode.name,
                initialContext = mode.context,
                onDismiss = { dialogMode = null },
                onSave = { name, paramContext ->
                    viewModel.updateCustomTemplate(context, mode.id, name, paramContext)
                    dialogMode = null
                }
            )
        }
    }
}

@Composable
private fun TemplateEditDialog(
    title: String,
    initialName: String,
    initialContext: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var paramContext by remember { mutableStateOf(initialContext) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.template_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = paramContext,
                    onValueChange = { paramContext = it },
                    label = { Text(stringResource(R.string.ffmpeg_parameters)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && paramContext.isNotBlank()) {
                        onSave(name.trim(), paramContext.trim())
                    }
                },
                enabled = name.isNotBlank() && paramContext.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun startSelfFFmpegCommandsTask(
    ffmpegCommandsViewModel: FFmpegCommandsViewModel,
    ctx: Context,
    nextDestination: () -> Unit,
) {
    val command_arg_list = ffmpegCommandsViewModel.command_args_str.value.trim().split("[\\s\\n]+".toRegex())
    val int_arr = ArrayList<Int>()
    int_arr.add(2)
    int_arr.add(command_arg_list.size)
    val long_arr = ArrayList<Long>()
    val str_arr = ArrayList<String>()
    val date = Date(System.currentTimeMillis())
    val formatter = SimpleDateFormat("yyyyMMddHHmmss", ctx.resources.configuration.locales[0])
    val task_log_path = ConfigsUtils.target_dir + "/" + ctx.getString(R.string.ffmpeg_command_line) + formatter.format(date) + ".log"
    ffmpegCommandsViewModel.task_log_path = task_log_path
    str_arr.add(task_log_path)
    str_arr.addAll(command_arg_list)
    val float_arr = ArrayList<Float>()
    val info = TaskInfo(
        int_arr,
        long_arr,
        str_arr,
        float_arr
    )
    AppApplication.INSTANCE.taskRepository.startNewTask(info)
    nextDestination()
}

// 🌟 4. 重构：支持单文件/多文件的批量任务并发提交
private fun startAssistedFFmpegCommandsTask(
    viewModel: FFmpegCommandsViewModel,
    ctx: Context,
    nextDestination: () -> Unit,
) {
    val files = viewModel.selectedInputFiles
    if (files.isEmpty()) return

    val isBatch = files.size > 1

    files.forEachIndexed { index, file ->
        val outFileName = if (isBatch) {
            "${viewModel.output_file_name}_${index + 1}.${viewModel.extensionText}"
        } else {
            "${viewModel.output_file_name}.${viewModel.extensionText}"
        }

        val date = Date(System.currentTimeMillis())
        val formatter = SimpleDateFormat("yyyyMMddHHmmss", ctx.resources.configuration.locales[0])
        val taskLogPath =if (isBatch) {
            "${ConfigsUtils.target_dir}/${ctx.getString(R.string.ffmpeg_command_line)}${formatter.format(date)}_${index + 1}.log"
        }
        else{
            "${ConfigsUtils.target_dir}/${ctx.getString(R.string.ffmpeg_command_line)}${formatter.format(date)}.log"
        }
        val cmdStr = "ffmpeg -i input_file ${viewModel.parameterContextText} output_file"
        val commandArgList = cmdStr
            .trim()
            .split("[\\s\\n]+".toRegex())
            .map {
                when (it) {
                    "input_file" -> file.path
                    "output_file" -> "${ConfigsUtils.target_dir}/$outFileName"
                    else -> it
                }
            }

        val intArr = ArrayList<Int>()
        intArr.add(2)
        intArr.add(commandArgList.size)
        val longArr = ArrayList<Long>()
        val strArr = ArrayList<String>()

        strArr.add(taskLogPath)
        strArr.addAll(commandArgList)
        val floatArr = ArrayList<Float>()

        val info = TaskInfo(
            intArr,
            longArr,
            strArr,
            floatArr
        )
        AppApplication.INSTANCE.taskRepository.startNewTask(info)
    }

    viewModel.clearInputFiles()
    viewModel.output_file_name = ""
    nextDestination()
}