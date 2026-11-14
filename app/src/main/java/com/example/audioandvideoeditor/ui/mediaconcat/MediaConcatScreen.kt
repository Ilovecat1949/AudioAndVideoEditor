package com.example.audioandvideoeditor.ui.mediaconcat

import android.content.Context
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.application.AppApplication
import com.example.audioandvideoeditor.entity.MediaInfo
import com.example.audioandvideoeditor.entity.TaskInfo
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.navigation.Destination
import com.example.audioandvideoeditor.utils.ConfigsUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaConcatScreen(
    onTaskSubmitted: () -> Unit,
    onNavigateToFilePicker: (route: String) -> Unit,
    onSaveReturnRoute: (route: String) -> Unit,
    pickedFiles: List<File>?,
    concatViewModel: MediaConcatViewModel = viewModel()
) {
    val life = rememberLifecycle()
    val context = LocalContext.current

    life.onLifeCreate {
        concatViewModel.appendPickedFiles(pickedFiles)

        if (concatViewModel.outputFileName.isEmpty()) {
            val formatter = SimpleDateFormat(
                "yyyyMMddHHmmss",
                context.resources.configuration.locales[0]
            )
            concatViewModel.outputFileName = "concat_${formatter.format(Date())}"
        }
    }

    val files = concatViewModel.fileList

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 媒体类型选择
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.media_concat_media_type_label) + ":",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = concatViewModel.mediaType == ConcatMediaType.VIDEO,
                            onClick = { concatViewModel.switchMediaType(ConcatMediaType.VIDEO) },
                            label = { Text(stringResource(id = R.string.media_concat_type_video)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = concatViewModel.mediaType == ConcatMediaType.AUDIO,
                            onClick = { concatViewModel.switchMediaType(ConcatMediaType.AUDIO) },
                            label = { Text(stringResource(id = R.string.media_concat_type_audio)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. 文件顺序列表与排序区
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
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
                            text = stringResource(id = R.string.media_concat_files_header),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        if (files.isNotEmpty()) {
                            TextButton(onClick = { concatViewModel.clearAllFiles() }) {
                                Text(
                                    text = stringResource(id = R.string.media_concat_clear_all),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        IconButton(onClick = {
                            onNavigateToFilePicker(Destination.MultiFileSelection.route)
                            onSaveReturnRoute(Destination.MediaConcat.route)
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_folder_24),
                                contentDescription = stringResource(id = R.string.media_concat_add_files_btn),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (files.isEmpty()) {
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            placeholder = { Text(stringResource(id = R.string.media_concat_empty_hint)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onNavigateToFilePicker(Destination.MultiFileSelection.route)
                                    onSaveReturnRoute(Destination.MediaConcat.route)
                                }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(4.dp)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(files) { index, file ->
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
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 4.dp)
                                        )
                                        IconButton(
                                            onClick = { concatViewModel.moveItemUp(index) },
                                            enabled = index > 0,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = stringResource(id = R.string.media_concat_move_up),
                                                tint = if (index > 0) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                            )
                                        }
                                        IconButton(
                                            onClick = { concatViewModel.moveItemDown(index) },
                                            enabled = index < files.size - 1,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = stringResource(id = R.string.media_concat_move_down),
                                                tint = if (index < files.size - 1) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                            )
                                        }
                                        IconButton(
                                            onClick = { concatViewModel.removeFileAt(index) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                                                contentDescription = stringResource(id = R.string.delete),
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
        }

        // 3. 拼接引擎模式与兼容模式下的分辨率策略选择
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.media_concat_mode_label) + ":",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { concatViewModel.engineMode = ConcatEngineMode.FAST_DEMUXER },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = concatViewModel.engineMode == ConcatEngineMode.FAST_DEMUXER,
                            onClick = { concatViewModel.engineMode = ConcatEngineMode.FAST_DEMUXER }
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                            Text(
                                text = stringResource(id = R.string.media_concat_mode_fast),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(id = R.string.media_concat_mode_fast_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { concatViewModel.engineMode = ConcatEngineMode.COMPATIBLE_FILTER },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = concatViewModel.engineMode == ConcatEngineMode.COMPATIBLE_FILTER,
                            onClick = { concatViewModel.engineMode = ConcatEngineMode.COMPATIBLE_FILTER }
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                            Text(
                                text = stringResource(id = R.string.media_concat_mode_compat),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(id = R.string.media_concat_mode_compat_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 如果当前是通用兼容模式且处于视频拼接，展示画布分辨率手动选择下拉框
                    if (concatViewModel.engineMode == ConcatEngineMode.COMPATIBLE_FILTER && concatViewModel.mediaType == ConcatMediaType.VIDEO) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.media_concat_resolution_label) + ":",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        ResolutionDropdown(viewModel = concatViewModel)
                    }
                }
            }
        }

        // 4. 输出文件命名区
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.media_concat_output_name_label) + ":",
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
                        value = concatViewModel.outputFileName,
                        onValueChange = { concatViewModel.outputFileName = it },
                        singleLine = true,
                        modifier = Modifier.width((width * 0.58).dp)
                    )
                    ConcatExtensionDropdown(
                        viewModel = concatViewModel,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. 底部执行按钮
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        submitMediaConcatTaskType3(concatViewModel, context, onTaskSubmitted)
                    },
                    enabled = concatViewModel.canStartConcat,
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.media_concat_start_btn, files.size),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResolutionDropdown(viewModel: MediaConcatViewModel) {
    val resolutionOptions = listOf(
        stringResource(id = R.string.media_concat_res_auto),
        stringResource(id = R.string.media_concat_res_1080p),
        stringResource(id = R.string.media_concat_res_720p),
        stringResource(id = R.string.media_concat_res_vertical)
    )
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = resolutionOptions[viewModel.selectedResolutionIndex],
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
            resolutionOptions.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        viewModel.selectedResolutionIndex = index
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConcatExtensionDropdown(
    viewModel: MediaConcatViewModel,
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
            viewModel.currentExtensionOptions.forEach { ext ->
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

/**
 * 终极核心升级：
 * 1. 严格交替组装 [v0][a0][v1][a1]... 解决 Media type mismatch。
 * 2. 支持智能自适应首个视频分辨率 或 手动目标画布分辨率。
 * 3. 采用 scale + pad 保留完整画幅、等比居中加黑边。
 * 4. 采用类型 3（Type 3）管控输出路径与总时长进度条。
 */
private fun submitMediaConcatTaskType3(
    viewModel: MediaConcatViewModel,
    context: Context,
    onTaskSubmitted: () -> Unit
) {
    val files = viewModel.fileList.toList()
    if (files.size < 2) return

    val date = Date(System.currentTimeMillis())
    val timestamp = SimpleDateFormat("yyyyMMddHHmmss", context.resources.configuration.locales[0]).format(date)
    val taskLogPath = "${context.filesDir.absolutePath}/ffmpeg_concat_$timestamp.log"
    val outputPath = "${ConfigsUtils.target_dir}/${viewModel.outputFileName.trim()}.${viewModel.selectedExtension}"

    val commandArgList = mutableListOf("ffmpeg")
    var totalDurationMs = 0L

    // 1. 预读所有文件的 MediaInfo
    val fileMediaInfos = files.map { file ->
        val infoStr = AppApplication.INSTANCE.taskRepository.getAVInfo(file.absolutePath)
        val mediaInfo = MediaInfo().apply { initInfo(infoStr) }

        val fileDur = if (mediaInfo.duration > 0) {
            mediaInfo.duration * 1000L
        } else {
            0L
        }
        totalDurationMs += fileDur

        Pair(file, mediaInfo)
    }

    when (viewModel.engineMode) {
        ConcatEngineMode.FAST_DEMUXER -> {
            // 1. 固定文件名，并在生成前先删除旧文件，防止历史积累
            val listFile = File(context.cacheDir, "ffmpeg_concat_list.txt")
            if (listFile.exists()) {
                listFile.delete()
            }

            // 2. 组装并写入列表内容
            val listContent = buildString {
                fileMediaInfos.forEach { (file, _) ->
                    val escapedPath = file.absolutePath.replace("'", "'\\''")
                    append("file '$escapedPath'\n")
                }
            }
            listFile.writeText(listContent, Charsets.UTF_8)

            commandArgList.addAll(
                listOf(
                    "-f", "concat",
                    "-safe", "0",
                    "-i", listFile.absolutePath,
                    "-c", "copy",
                    outputPath
                )
            )
        }

        ConcatEngineMode.COMPATIBLE_FILTER -> {
            // 输入文件依次添加
            fileMediaInfos.forEach { (file, _) ->
                commandArgList.add("-i")
                commandArgList.add(file.absolutePath)
            }

            val n = files.size
            val filterBuilder = StringBuilder()
            val concatInputsBuilder = StringBuilder()

            // 确定目标分辨率 (Width & Height)
            var targetW = 1280
            var targetH = 720

            if (viewModel.mediaType == ConcatMediaType.VIDEO) {
                when (viewModel.selectedResolutionIndex) {
                    0 -> {
                        // 智能自适应：跟随首个视频的分辨率
                        val firstInfo = fileMediaInfos.firstOrNull()?.second
                        if (firstInfo != null && firstInfo.width > 0 && firstInfo.height > 0) {
                            // 确保宽高为偶数（部分编码器对奇数分辨率报错）
                            targetW = if (firstInfo.width % 2 != 0) firstInfo.width + 1 else firstInfo.width
                            targetH = if (firstInfo.height % 2 != 0) firstInfo.height + 1 else firstInfo.height
                        }
                    }
                    1 -> { targetW = 1920; targetH = 1080 } // 1080p
                    2 -> { targetW = 1280; targetH = 720 }  // 720p
                    3 -> { targetW = 1080; targetH = 1920 } // 竖屏
                }
            }

            // 🌟 核心修复：按交替顺序构建滤镜链 [v0][a0][v1][a1]...
            fileMediaInfos.forEachIndexed { i, (_, info) ->
                val hasVideo = info.video_codec_type.isNotBlank() || info.width > 0
                val hasAudio = info.audio_codec_type.isNotBlank() || info.channels > 0
                val durSec = (info.duration * 1000L / 1000.0).coerceAtLeast(0.1)

                if (viewModel.mediaType == ConcatMediaType.VIDEO) {
                    // 1. 视频流处理：等比缩放 + 加黑边居中 (Letterbox) + 统一帧率
                    if (hasVideo) {
                        filterBuilder.append("[$i:v]scale=$targetW:$targetH:force_original_aspect_ratio=decrease,pad=$targetW:$targetH:(ow-iw)/2:(oh-ih)/2,setsar=1,fps=30[v$i];")
                    } else {
                        // 纯音频兜底视频轨
                        filterBuilder.append("color=c=black:s=${targetW}x${targetH}:r=30:d=$durSec,trim=duration=$durSec,setpts=PTS-STARTPTS[v$i];")
                    }
                    concatInputsBuilder.append("[v$i]")

                    // 2. 音频流处理：统一采样率与声道
                    if (hasAudio) {
                        filterBuilder.append("[$i:a]aformat=sample_fmts=fltp:sample_rates=44100:channel_layouts=stereo[a$i];")
                    } else {
                        // 无声视频兜底音频轨
                        filterBuilder.append("anullsrc=r=44100:cl=stereo,atrim=duration=$durSec[a$i];")
                    }
                    concatInputsBuilder.append("[a$i]")
                } else {
                    // 纯音频拼接
                    if (hasAudio) {
                        filterBuilder.append("[$i:a]aformat=sample_fmts=fltp:sample_rates=44100:channel_layouts=stereo[a$i];")
                    } else {
                        filterBuilder.append("anullsrc=r=44100:cl=stereo[a$i];")
                    }
                    concatInputsBuilder.append("[a$i]")
                }
            }

            // 附加 concat 节点
            if (viewModel.mediaType == ConcatMediaType.VIDEO) {
                filterBuilder.append("${concatInputsBuilder}concat=n=$n:v=1:a=1[outv][outa]")
                commandArgList.addAll(
                    listOf(
                        "-filter_complex", filterBuilder.toString(),
                        "-map", "[outv]",
                        "-map", "[outa]",
                        "-c:v", "libx264",
                        "-preset", "veryfast",
                        "-crf", "23",
                        "-c:a", "aac",
                        "-b:a", "128k",
                        outputPath
                    )
                )
            } else {
                filterBuilder.append("${concatInputsBuilder}concat=n=$n:v=0:a=1[outa]")

                // 🌟 根据选定的音频后缀名动态适配音频编码器
                val audioEncoder = when (viewModel.selectedExtension.lowercase()) {
                    "mp3" -> "libmp3lame"
                    "wav" -> "pcm_s16le"
                    else -> "aac"
                }

                val args = mutableListOf(
                    "-vn",
                    "-filter_complex", filterBuilder.toString(),
                    "-map", "[outa]",
                    "-c:a", audioEncoder
                )

                // wav 通常不需要设置比特率参数，mp3/aac/m4a 需要
                if (audioEncoder != "pcm_s16le") {
                    args.addAll(listOf("-b:a", if (audioEncoder == "libmp3lame") "192k" else "128k"))
                }

                args.add(outputPath)
                commandArgList.addAll(args)
            }
        }
    }

    // ==========================================
    // 🌟 构建【任务类型 3 (Type 3)】管道数据结构
    // ==========================================
    val intArr = ArrayList<Int>().apply {
        add(3)
        add(commandArgList.size)
    }

    val longArr = ArrayList<Long>().apply {
        add(totalDurationMs) // 传入总时长支持百分比进度条
    }

    val strArr = ArrayList<String>().apply {
        add(outputPath)      // 索引 0：确定的输出文件路径
        add(taskLogPath)     // 索引 1：日志路径
        addAll(commandArgList)// 索引 2+：命令参数
    }

    val floatArr = ArrayList<Float>()

    val info = TaskInfo(intArr, longArr, strArr, floatArr)
    AppApplication.INSTANCE.taskRepository.startNewTask(info)

    viewModel.resetState()
    onTaskSubmitted()
}