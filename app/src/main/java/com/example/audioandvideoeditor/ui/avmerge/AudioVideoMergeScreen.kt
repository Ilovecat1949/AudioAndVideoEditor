package com.example.audioandvideoeditor.ui.avmerge

import android.content.Context
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun AudioVideoMergeScreen(
    onTaskSubmitted: () -> Unit,
    onNavigateToFilePicker: (route: String) -> Unit,
    onSaveReturnRoute: (route: String) -> Unit,
    pickedFiles: File,
    mergeViewModel: AudioVideoMergeViewModel = viewModel()
) {
    val life = rememberLifecycle()
    val context = LocalContext.current

    life.onLifeCreate {
        // 接收从文件选择器返回的文件
        mergeViewModel.onFilePicked(pickedFiles)

        // 自动生成默认时间戳文件名
        if (mergeViewModel.outputFileName.isEmpty()) {
            val formatter = SimpleDateFormat(
                "yyyyMMddHHmmss",
                context.resources.configuration.locales[0]
            )
            mergeViewModel.outputFileName = "merge_${formatter.format(Date())}"
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // 1. 视频选择卡片
            MediaSlotCard(
                title = stringResource(id = R.string.av_merge_video_slot),
                hint = stringResource(id = R.string.av_merge_select_video_hint),
                selectedFile = mergeViewModel.selectedVideoFile,
                onPickClick = {
                    mergeViewModel.currentPickingTarget = MergePickingTarget.VIDEO
                    onNavigateToFilePicker(Destination.FileSelection.route)
                    onSaveReturnRoute(Destination.AudioVideoMerge.route)
                },
                onClearClick = { mergeViewModel.clearVideoFile() }
            )
        }

        item {
            // 2. 音频选择卡片
            MediaSlotCard(
                title = stringResource(id = R.string.av_merge_audio_slot),
                hint = stringResource(id = R.string.av_merge_select_audio_hint),
                selectedFile = mergeViewModel.selectedAudioFile,
                onPickClick = {
                    mergeViewModel.currentPickingTarget = MergePickingTarget.AUDIO
                    onNavigateToFilePicker(Destination.FileSelection.route)
                    onSaveReturnRoute(Destination.AudioVideoMerge.route)
                },
                onClearClick = { mergeViewModel.clearAudioFile() }
            )
        }

        item {
            // 3. 合成参数选项卡片
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 开关 1：自动对齐较短时长 (-shortest)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                mergeViewModel.isShortestEnabled = !mergeViewModel.isShortestEnabled
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = stringResource(id = R.string.av_merge_shortest_option),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(id = R.string.av_merge_shortest_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = mergeViewModel.isShortestEnabled,
                            onCheckedChange = { mergeViewModel.isShortestEnabled = it }
                        )
                    }

                    // 开关 2：是否替换原视频音轨 (-map 0:v:0 -map 1:a:0 vs amix)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                mergeViewModel.isReplaceOriginalAudio = !mergeViewModel.isReplaceOriginalAudio
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(id = R.string.av_merge_replace_audio_option),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = mergeViewModel.isReplaceOriginalAudio,
                            onCheckedChange = { mergeViewModel.isReplaceOriginalAudio = it }
                        )
                    }
                }
            }
        }

        item {
            // 4. 输出文件命名区
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.av_merge_output_name_label) + ":",
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
                        value = mergeViewModel.outputFileName,
                        onValueChange = { mergeViewModel.outputFileName = it },
                        singleLine = true,
                        modifier = Modifier.width((width * 0.58).dp)
                    )
                    ExtensionDropdown(
                        viewModel = mergeViewModel,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            // 5. 底部执行按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        submitAudioVideoMergeTask(mergeViewModel, context, onTaskSubmitted)
                    },
                    enabled = mergeViewModel.canStartMerge,
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.av_merge_start_btn),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaSlotCard(
    title: String,
    hint: String,
    selectedFile: File?,
    onPickClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPickClick() },
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$title:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onPickClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_folder_24),
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (selectedFile == null) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    placeholder = { Text(hint) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = selectedFile.name,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClearClick) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtensionDropdown(
    viewModel: AudioVideoMergeViewModel,
    modifier: Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = viewModel.selectedExtension,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            viewModel.extensionOptions.forEach { ext ->
                DropdownMenuItem(
                    text = { Text(ext) },
                    onClick = {
                        viewModel.selectedExtension = ext
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun submitAudioVideoMergeTask(
    viewModel: AudioVideoMergeViewModel,
    context: Context,
    onTaskSubmitted: () -> Unit
) {
    val videoFile = viewModel.selectedVideoFile ?: return
    val audioFile = viewModel.selectedAudioFile ?: return

    val date = Date(System.currentTimeMillis())
    val formatter = SimpleDateFormat("yyyyMMddHHmmss", context.resources.configuration.locales[0])
    val taskLogPath = "${context.filesDir.absolutePath}/ffmpeg_avmerge_${formatter.format(date)}.log"
    val outputPath = "${ConfigsUtils.target_dir}/${viewModel.outputFileName.trim()}.${viewModel.selectedExtension}"

    // 1. 分别预读视频和音频文件的 MediaInfo
    val videoInfoStr = AppApplication.INSTANCE.taskRepository.getAVInfo(videoFile.absolutePath)
    val videoMediaInfo = com.example.audioandvideoeditor.entity.MediaInfo().apply { initInfo(videoInfoStr) }

    val audioInfoStr = AppApplication.INSTANCE.taskRepository.getAVInfo(audioFile.absolutePath)
    val audioMediaInfo = com.example.audioandvideoeditor.entity.MediaInfo().apply { initInfo(audioInfoStr) }


    val videoDurMs = videoMediaInfo.duration*1000L
    val audioDurMs = audioMediaInfo.duration*1000L

    val videoHasAudio = videoMediaInfo.audio_codec_type.isNotBlank() || videoMediaInfo.channels > 0
    val effectiveReplace = viewModel.isReplaceOriginalAudio || !videoHasAudio
    // 3. 🌟 结合用户是否开启 isShortestEnabled 智能计算最终合成总时长
    val totalDurationMs = when {
        videoDurMs <= 0 && audioDurMs <= 0 -> 0L
        // 混音模式：amix duration=first 以视频原音轨为准
        !effectiveReplace -> videoDurMs.takeIf { it > 0 } ?: audioDurMs
        // 替换模式：看 -shortest
        viewModel.isShortestEnabled -> minOf(videoDurMs, audioDurMs)
        else -> maxOf(videoDurMs, audioDurMs)
    }

    // 4. 防御性组装参数列表
    val commandArgList = mutableListOf(
        "ffmpeg",
        "-i", videoFile.path,
        "-i", audioFile.path
    )

    if (effectiveReplace) {
        commandArgList.addAll(listOf("-c:v", "copy", "-c:a", "aac", "-map", "0:v:0", "-map", "1:a:0"))
    } else {
        commandArgList.addAll(
            listOf(
                "-filter_complex", "[0:a][1:a]amix=inputs=2:duration=first:dropout_transition=2[aout]",
                "-map", "0:v:0",
                "-map", "[aout]",
                "-c:v", "copy",
                "-c:a", "aac"
            )
        )
    }

    if (viewModel.isShortestEnabled) {
        commandArgList.add("-shortest")
    }

    commandArgList.add(outputPath)

    // 5. 构建【任务类型 3 (Type 3)】管道数据结构
    val intArr = ArrayList<Int>().apply {
        add(3)
        add(commandArgList.size)
    }

    val longArr = ArrayList<Long>().apply {
        add(totalDurationMs) // 动态精准计算出的总时长
    }

    val strArr = ArrayList<String>().apply {
        add(outputPath)
        add(taskLogPath)
        addAll(commandArgList)
    }

    val floatArr = ArrayList<Float>()

    val info = TaskInfo(intArr, longArr, strArr, floatArr)
    AppApplication.INSTANCE.taskRepository.startNewTask(info)

    viewModel.resetState()
    onTaskSubmitted()
}