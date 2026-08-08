package com.example.audioandvideoeditor.ui.avinfo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.utils.LogUtils.showToast
import com.example.audioandvideoeditor.utils.TextsUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun AVInfoScreen(
    path: String,
    nextDestination: () -> Unit,
    avInfoViewModel: AudioAndVideoInfoViewModel = viewModel()
) {
    val life = rememberLifecycle()
    life.onLifeCreate {
        avInfoViewModel.getInfo(path)
    }

    val file = File(path)
    val isEdit = avInfoViewModel.isEditMode
    val currentInfo = if (isEdit) avInfoViewModel.draftInfo else avInfoViewModel.info
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部简易动作 Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.edit_media_info),
                    fontWeight = FontWeight.Bold
                )
                if (!isEdit) {
                    Button(onClick = { avInfoViewModel.startEditing() }) {
                        Text(stringResource(R.string.edit))
                    }
                } else {
                    Row {
                        Button(onClick = { avInfoViewModel.cancelEditing() }) {
                            Text(stringResource(R.string.cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                avInfoViewModel.onSaveClicked(path) { success ->
                                    avInfoViewModel.cancelEditing()
                                    if (success) {
                                        context.showToast(R.string.status_success)
                                    } else {
                                        context.showToast(R.string.fail)
                                    }
                                }
                            },
                            enabled = !avInfoViewModel.isProcessing
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }

            Divider(color = Color.LightGray, thickness = 1.dp)

            // 主属性列表[cite: 3]
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 1. 基础文件信息[cite: 3]
                        showFileAttributeItem(stringResource(id = R.string.file_name), file.name)
                        showFileAttributeItem(stringResource(id = R.string.path), file.path)

                        // 可编辑项：标题 (Title)[cite: 3]
                        EditableAttributeItem(
                            attribute = stringResource(R.string.media_title),
                            value = currentInfo.title,
                            isEditing = isEdit,
                            onValueChange = { avInfoViewModel.updateDraftTitle(it) }
                        )

                        if (currentInfo.file_size > 0L) {
                            showFileAttributeItem(stringResource(id = R.string.size), TextsUtils.getSizeText(currentInfo.file_size))
                        } else {
                            showFileAttributeItem(stringResource(id = R.string.size), TextsUtils.getSizeText(file.length()))
                        }

                        val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
                        showFileAttributeItem(stringResource(id = R.string.date), sdf.format(file.lastModified()))
                        showFileAttributeItem(stringResource(id = R.string.extension), file.extension)

                        if (currentInfo.format_name.isNotEmpty()) {
                            showFileAttributeItem(stringResource(R.string.container_format), currentInfo.format_name)
                        }
                        if (currentInfo.duration > 0L) {
                            showFileAttributeItem(stringResource(R.string.total_duration), TextsUtils.millisecondsToString(currentInfo.duration * 1000))
                        }
//                        EditableAttributeItem(
//                            attribute = stringResource(id = R.string.creation_time),
//                            value = currentInfo.creation_time,
//                            isEditable = isEdit,
//                            onValueChange = { avInfoViewModel.updateDraftCreationTime(it) }
//                        )

                        EditableAttributeItem2(
                            attribute = stringResource(id = R.string.creation_time),
                            value = currentInfo.creation_time,
                            defaultValue = stringResource(R.string.not_set),
                            isEditing = isEdit,
                            onClick = {
                                avInfoViewModel.showCreationTimePickerDialog=true
                            },
                            trailingIcon =
                                {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = stringResource(R.string.title_select_time),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                        )

                        // 可编辑项：艺术家 (Artist)[cite: 3]
                        EditableAttributeItem(
                            attribute = stringResource(id = R.string.artist),
                            value = currentInfo.artist,
                            isEditing = isEdit,
                            onValueChange = { avInfoViewModel.updateDraftArtist(it) }
                        )

                        // 可编辑项：专辑
                        EditableAttributeItem(
                            attribute = stringResource(id = R.string.album),
                            value = currentInfo.album,
                            isEditing = isEdit,
                            onValueChange = { avInfoViewModel.updateDraftAlbum(it)  }
                        )
                        // 可编辑项：流派
                        EditableAttributeItem(
                            attribute = stringResource(id = R.string.genre),
                            value = currentInfo.genre,
                            isEditing = isEdit,
                            onValueChange = { avInfoViewModel.updateDraftGenre(it) }
                        )


                        // 可编辑项：描述 (Description)[cite: 3]
                        EditableAttributeItem(
                            attribute = stringResource(id = R.string.description),
                            value = currentInfo.description,
                            isEditing = isEdit,
                            onValueChange = { avInfoViewModel.updateDraftDescription(it) }
                        )

                        EditableAttributeItem(
                            attribute = stringResource(id = R.string.copyright),
                            value = currentInfo.copyright,
                            isEditing = isEdit,
                            onValueChange = { avInfoViewModel.updateDraftCopyright(it) }
                        )

//                        if (currentInfo.copyright.isNotEmpty() || isEdit) {
//                            showFileAttributeItem(stringResource(id = R.string.copyright), currentInfo.copyright)
//                        }
                        if (currentInfo.encoder.isNotEmpty()) {
                            showFileAttributeItem(stringResource(id = R.string.encoder), currentInfo.encoder)
                        }
                    }
                }

                // 2. 视频流属性块[cite: 3]
                if (currentInfo.video_codec_type.isNotEmpty()) {
                    item {
                        showFileAttributeItem(stringResource(id = R.string.video_duration), TextsUtils.millisecondsToString(currentInfo.video_duration * 1000))

                        // 可编辑项：分辨率 (Width x Height) - 重度编辑
//                        EditableAttributeItem(
//                            attribute = stringResource(id = R.string.video_resolution),
//                            value = "${currentInfo.width}x${currentInfo.height}",
//                            isEditable = isEdit,
//                            onValueChange = { resStr ->
//                                val parts = resStr.split("x", "X", ":")
//                                if (parts.size == 2) {
//                                    avInfoViewModel.updateDraftResolution(parts[0], parts[1])
//                                }
//                            }
//                        )
                        EditableAttributeItem2(
                            attribute = stringResource(id = R.string.video_resolution),
                            value = "${currentInfo.width}x${currentInfo.height}",
                            defaultValue = stringResource(R.string.not_set),
                            isEditing = isEdit,
                            onClick = {
                                avInfoViewModel.showResolutionPickerDialog =true
                            },
                            trailingIcon =
                                {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.edit),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                        )

                        // 可编辑项：视频码率 (kb/s) - 重度编辑
                        EditableAttributeItem(
                            attribute = "${stringResource(id = R.string.video_bit_rate)}(kb/s)",
                            value = if(currentInfo.video_bit_rate!=-1L) "${currentInfo.video_bit_rate / 1000}" else "",
                            isEditing = isEdit,
                            onValueChange = { avInfoViewModel.updateDraftVideoBitrate(it) }
                        )

                        // 可编辑项：帧率 (fps) - 重度编辑
                        EditableAttributeItem(
                            attribute = "${stringResource(id = R.string.frame_rate)}(fps)",
                            value =if(currentInfo.frame_rate!=-1f) String.format(Locale.US, "%.2f", currentInfo.frame_rate) else "",
                            isEditing = isEdit,
                            onValueChange = { avInfoViewModel.updateDraftFps(it) }
                        )
                        // 视频编码格式
                        EditableAttributeItem2(
                            attribute = stringResource(id = R.string.video_encoding_format),
                            value = currentInfo.video_codec_type,
                            defaultValue = stringResource(R.string.not_set),
                            isEditing = isEdit,
                            onClick = { avInfoViewModel.showVideoCodecPickerModal = true },
                            trailingIcon = {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        )

                        // 可编辑项：视频旋转角度 (Rotation - 点击递增切换)[cite: 3]
                        if (isEdit) {
                            EditableAttributeItem(
                                attribute = stringResource(R.string.video_rotation),
                                value = "${currentInfo.rotation}° (${stringResource(R.string.click_to_rotate)})",
                                isEditing = false,
                                modifier = Modifier.clickable { avInfoViewModel.rotateDraft() }
                            )
                        } else if (currentInfo.rotation != 0) {
                            showFileAttributeItem(stringResource(R.string.video_rotation), "${currentInfo.rotation}°")
                        }

                        // 视频 Profile
                        EditableAttributeItem2(
                            attribute = stringResource(R.string.video_profile),
                            value = currentInfo.video_profile,
                            defaultValue = stringResource(R.string.not_set),
                            isEditing = isEdit,
                            onClick = { avInfoViewModel.showVideoProfilePickerModal = true },
                            trailingIcon = {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        )
                        // 像素格式
                        EditableAttributeItem2(
                            attribute = stringResource(R.string.pixel_format),
                            value = currentInfo.pixel_format,
                            defaultValue = stringResource(R.string.not_set),
                            isEditing = isEdit,
                            onClick = { avInfoViewModel.showPixelFormatPickerModal = true },
                            trailingIcon = {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        )
                        if (currentInfo.color_space.isNotEmpty()) {
                            showFileAttributeItem(stringResource(R.string.color_space), currentInfo.color_space)
                        }
                        if (currentInfo.color_transfer.isNotEmpty()) {
                            showFileAttributeItem(stringResource(R.string.color_transfer), currentInfo.color_transfer)
                        }
                        if (currentInfo.sar_num > 0 && currentInfo.sar_den > 0) {
                            showFileAttributeItem(stringResource(R.string.sample_aspect_ratio), "${currentInfo.sar_num}:${currentInfo.sar_den}")
                        }
                    }
                }

                // 3. 音频流属性块[cite: 3]
                if (currentInfo.audio_codec_type.isNotBlank()) {
                    item {
                        showFileAttributeItem(stringResource(id = R.string.audio_duration), TextsUtils.millisecondsToString(currentInfo.audio_duration * 1000))

                        // 音频编码格式
                        EditableAttributeItem2(
                            attribute = stringResource(id = R.string.audio_format),
                            value = currentInfo.audio_codec_type,
                            defaultValue = stringResource(R.string.not_set),
                            isEditing = isEdit,
                            onClick = { avInfoViewModel.showAudioCodecPickerModal = true },
                            trailingIcon = {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        )

                        showFileAttributeItem(
                            "${stringResource(id = R.string.audio_bit_rate)}(kb/s)",
                            "${currentInfo.audio_bit_rate / 1000}"
                        )

                        // 可编辑项：采样率 (HZ) - 重度编辑
                        // 可编辑项：采样率 (HZ) - 点击弹出单选框
                        EditableAttributeItem2(
                            attribute = "${stringResource(id = R.string.sample_rate)}(HZ)",
                            value = if (currentInfo.sample_rate != -1) "${currentInfo.sample_rate}" else "",
                            defaultValue = stringResource(R.string.not_set),
                            isEditing = isEdit,
                            onClick = { avInfoViewModel.showSampleRatePickerModal = true },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )

                        // 可编辑项：音频 Profile
//                        EditableAttributeItem2(
//                            attribute = stringResource(id = R.string.audio_profile),
//                            value = currentInfo.audio_profile,
//                            defaultValue = stringResource(R.string.not_set),
//                            isEditing = isEdit,
//                            onClick = { avInfoViewModel.showAudioProfilePickerModal = true },
//                            trailingIcon = {
//                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
//                            }
//                        )
                        if(currentInfo.audio_profile.isNotEmpty()){
                            showFileAttributeItem(stringResource(R.string.audio_profile), currentInfo.audio_profile)
                        }
                        if (currentInfo.sample_format.isNotEmpty()) {
                            showFileAttributeItem(stringResource(R.string.sample_format), currentInfo.sample_format)
                        }
                    }
                }

                // 4. 字幕流属性块[cite: 3]
                if (currentInfo.has_subtitle) {
                    item {
                        showFileAttributeItem(stringResource(R.string.has_subtitle), stringResource(R.string.yes))
                        if (currentInfo.subtitle_language.isNotEmpty()) {
                            showFileAttributeItem(stringResource(R.string.subtitle_language), currentInfo.subtitle_language)
                        }
                    }
                }
            }
        }

        // 重度编辑：另存为新文件对话框
        if (avInfoViewModel.showSaveAsDialog) {
            val date = Date(System.currentTimeMillis())
            val formatter = SimpleDateFormat(
                "yyyyMMddHHmmss",
                context.resources.configuration.locales[0]
            )
            var newFileName by remember {
                mutableStateOf("${formatter.format(date)}.${file.extension}")
            }

            AlertDialog(
                onDismissRequest = { avInfoViewModel.dismissSaveAsDialog() },
                title = { Text(stringResource(R.string.save_as_new_file)) },
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.heavy_edit_notice),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newFileName,
                            onValueChange = { newFileName = it },
                            label = { Text(stringResource(R.string.new_file_name)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newFileName.isNotBlank()) {
                                avInfoViewModel.submitHeavyTask(path, newFileName)
                                nextDestination()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.start_task))
                    }
                },
                dismissButton = {
                    Button(onClick = { avInfoViewModel.dismissSaveAsDialog() }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        // 处理中遮罩层（仅在轻度编辑 Fast Edit 时使用）
        if (avInfoViewModel.isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .pointerInput(Unit) {},
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.processing_please_do_not_leave),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
    if(avInfoViewModel.showCreationTimePickerDialog){
//        CreationTimePickerModal(
//            onDateSelected = {
//                avInfoViewModel.updateDraftCreationTime(it)
//            },
//           onDismiss = {
//               avInfoViewModel.showCreationTimePickerDialog=false
//           }
//        )
        CreationDateTimePickerModal(
            onDateTimeSelected = {
                avInfoViewModel.updateDraftCreationTime(it)
            },
            onDismiss = {
                avInfoViewModel.showCreationTimePickerDialog=false
            }
        )
    }
    if(avInfoViewModel.showResolutionPickerDialog){
        ResolutionPickerModal(
            currentInfo.width,
            currentInfo.height,
            {w,h->
                avInfoViewModel.updateDraftResolution(w.toString(), h.toString())
            },
            {
                avInfoViewModel.showResolutionPickerDialog=false
            }
        )
    }

// 1. 视频编码格式选择器
    if (avInfoViewModel.showVideoCodecPickerModal) {
        SingleSelectOptionModal(
            title = stringResource(R.string.video_encoding_format),
            options = avInfoViewModel.videoCodecMap.keys.toList(),// "vp8", "vp9"
            currentValue = currentInfo.video_codec_type,
            onOptionSelected = { avInfoViewModel.updateDraftVideoCodec(it) },
            onDismiss = { avInfoViewModel.showVideoCodecPickerModal = false }
        )
    }

// 2. 视频 Profile 选择器
    if (avInfoViewModel.showVideoProfilePickerModal) {
        SingleSelectOptionModal(
            title = stringResource(R.string.video_profile),
            options = listOf("baseline", "main", "high", "high10"),
            currentValue = currentInfo.video_profile,
            onOptionSelected = { avInfoViewModel.updateDraftVideoProfile(it) },
            onDismiss = { avInfoViewModel.showVideoProfilePickerModal = false }
        )
    }

// 3. 像素格式选择器
    if (avInfoViewModel.showPixelFormatPickerModal) {
        SingleSelectOptionModal(
            title = stringResource(R.string.pixel_format),
            options = listOf("yuv420p", "yuv422p", "yuv444p", "yuv420p10le", "nv12", "nv21"),
            currentValue = currentInfo.pixel_format,
            onOptionSelected = { avInfoViewModel.updateDraftPixelFormat(it) },
            onDismiss = { avInfoViewModel.showPixelFormatPickerModal = false }
        )
    }

// 4. 音频编码格式选择器
    if (avInfoViewModel.showAudioCodecPickerModal) {
        SingleSelectOptionModal(
            title = stringResource(R.string.audio_format),
            options = avInfoViewModel.audioCodecMap.keys.toList(),
            currentValue = currentInfo.audio_codec_type,
            onOptionSelected = { avInfoViewModel.updateDraftAudioCodec(it) },
            onDismiss = { avInfoViewModel.showAudioCodecPickerModal = false }
        )
    }

    if (avInfoViewModel.showAudioProfilePickerModal) {
        SingleSelectOptionModal(
            title = stringResource(id = R.string.audio_profile),
            options = avInfoViewModel.audioProfileOptions,
            currentValue= currentInfo.audio_profile,
            onOptionSelected = { selectedProfile ->
                avInfoViewModel.updateDraftAudioProfile(selectedProfile)
            },
            onDismiss = {
                avInfoViewModel.showAudioProfilePickerModal=false
            }
        )
    }

// 5. 音频采样率选择器
    if (avInfoViewModel.showSampleRatePickerModal) {
        SingleSelectOptionModal(
            title = "${stringResource(id = R.string.sample_rate)}(HZ)",
            options = avInfoViewModel.sampleRateOptions,
            currentValue = if (currentInfo.sample_rate != -1) currentInfo.sample_rate.toString() else "",
            onOptionSelected = { selectedRate ->
                avInfoViewModel.updateDraftSampleRateSelected(selectedRate)
            },
            onDismiss = {
                avInfoViewModel.showSampleRatePickerModal=false
            }
        )
    }
}

// 原有的只读属性渲染组件[cite: 3]
@Composable
private fun showFileAttributeItem(
    attribute: String,
    value: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                attribute,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(value)
                Spacer(modifier = Modifier.width(10.dp))
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Divider(color = Color.LightGray, thickness = 1.dp)
    }
}

// 兼顾浏览与编辑输入的轻量属性组件
@Composable
private fun EditableAttributeItem(
    attribute: String,
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!isEditing) {
        if (value.isNotEmpty()) {
            Box(modifier = modifier) {
                showFileAttributeItem(attribute, value)
            }
        }
    } else {
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    text=attribute,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                    },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
// 🌟 新增：设置输入文字在不同状态下的颜色
                focusedTextColor = MaterialTheme.colorScheme.primary,   // 聚焦时的输入文字颜色
                unfocusedTextColor = MaterialTheme.colorScheme.primary,                      // 未聚焦时的输入文字颜色
                disabledTextColor = MaterialTheme.colorScheme.primary,                        // 禁用时的输入文字颜色

                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                unfocusedIndicatorColor = Color.LightGray,
                focusedIndicatorColor = Color.LightGray,
                disabledIndicatorColor = Color.LightGray
            )
        )
    }
}

@Composable
private fun EditableAttributeItem2(
    attribute: String,
    value: String,
    defaultValue: String,
    isEditing: Boolean = false,              // 是否已被修改（高亮显示）
    onClick: (() -> Unit)? = null,          // 点击回调（为 null 表示不可编辑/点击）
    trailingIcon: @Composable (() -> Unit)? = null // 编辑状态或属性类型专属图标
) {
    if(value.isNotEmpty() || isEditing) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null && isEditing) {
                        Modifier.clickable(onClick = onClick)
                    } else Modifier
                )
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width(10.dp))

                // 属性名称：若被修改过，更换颜色提醒
                Text(
                    text = attribute,
                    fontWeight = FontWeight.Bold,
                    color = if (isEditing) MaterialTheme.colorScheme.primary else Color.Unspecified
                )

                Spacer(modifier = Modifier.width(10.dp))

                // 右侧数值与图标区域
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                     // 实际展示的文字：若为空则使用默认值
                    val displayValue = value.ifEmpty { defaultValue }
                    Text(
                        text = displayValue,
                        color = if (isEditing) MaterialTheme.colorScheme.primary else Color.Unspecified,
                        fontWeight = if (isEditing) FontWeight.Medium else FontWeight.Normal
                    )

                    // 渲染外部传入的专属图标（如日历、编辑铅笔等）
                    if (trailingIcon != null && isEditing) {
                        Spacer(modifier = Modifier.width(6.dp))
                        trailingIcon()
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreationTimePickerModal(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    // 1. 将选择的时间戳转为 ISO 标准字符串
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC") // 强制使用 UTC 时区
                    }
                    val isoString = sdf.format(Date(millis))
                    // 2. 回传给 ViewModel 保存到 editedMap 中
                    onDateSelected(isoString)
                }
                onDismiss()
            }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreationDateTimePickerModal(
    onDateTimeSelected: (iso8601String: String) -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val datePickerState = rememberDatePickerState()

    val calendar = remember { Calendar.getInstance() }
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
        is24Hour = true
    )

    if (step == 0) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = { step = 1 },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text(stringResource(R.string.btn_next))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val dateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()

                    // 1. 获取 DatePicker 选中的年月日 (注意：DatePickerState 返回的是 UTC 零点的时间戳)
                    val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = dateMillis
                    }
                    val year = utcCalendar.get(Calendar.YEAR)
                    val month = utcCalendar.get(Calendar.MONTH)
                    val day = utcCalendar.get(Calendar.DAY_OF_MONTH)

                    // 2. 使用【默认本地时区】组合 年、月、日 + TimePicker 选中的时、分
                    val localCalendar = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    // 3. 将这个准确的本地时刻，转换为标准 ISO 8601 UTC 字符串
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val isoString = sdf.format(localCalendar.time)

                    onDateTimeSelected(isoString)
                    onDismiss()
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { step = 0 }) { Text(stringResource(R.string.btn_previous)) }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.title_select_time),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}



@Composable
fun ResolutionPickerModal(
    currentWidth: Int?,
    currentHeight: Int?,
    onResolutionSelected: (width: Int, height: Int) -> Unit,
    onDismiss: () -> Unit
) {
    // 使用 Triple<宽度, 高度, 字符串资源ID>，不定义额外的类
    val presets = remember {
        listOf(
            Triple(1080, 1920, R.string.preset_1080p_vertical),
            Triple(1920, 1080, R.string.preset_1080p_horizontal),
            Triple(720, 1280, R.string.preset_720p_vertical),
            Triple(1280, 720, R.string.preset_720p_horizontal),
            Triple(480, 854, R.string.preset_480p_vertical),
            Triple(854, 480, R.string.preset_480p_horizontal)
        )
    }

    var isCustomMode by remember { mutableStateOf(false) }
    var selectedWidth by remember { mutableIntStateOf(currentWidth ?: 1080) }
    var selectedHeight by remember { mutableIntStateOf(currentHeight ?: 1920) }

    var customWidthText by remember { mutableStateOf((currentWidth ?: 1080).toString()) }
    var customHeightText by remember { mutableStateOf((currentHeight ?: 1920).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.title_select_resolution),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 模式切换
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
// 在 Modal 内部 FilterChip 模式切换点击事件中添加同步：
                    FilterChip(
                        selected = !isCustomMode,
                        onClick = {
                            isCustomMode = false
                            // 切回预设时，若 custom 框里的值合法，自动对齐 selectedWidth/Height
                            val parsedW = customWidthText.toIntOrNull()
                            val parsedH = customHeightText.toIntOrNull()
                            if (parsedW != null && parsedH != null) {
                                selectedWidth = ((parsedW.coerceAtLeast(2)) / 2) * 2
                                selectedHeight = ((parsedH.coerceAtLeast(2)) / 2) * 2
                            }
                        },
                        label = { Text(stringResource(R.string.label_presets)) }
                    )

                    FilterChip(
                        selected = isCustomMode,
                        onClick = {
                            isCustomMode = true
                            // 切到自定义模式时，将当前选择的预设同步到输入框文本中
                            customWidthText = selectedWidth.toString()
                            customHeightText = selectedHeight.toString()
                        },
                        label = { Text(stringResource(R.string.label_custom)) }
                    )
                }

                if (!isCustomMode) {
                    // 常用预设列表 (使用解构赋值)
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.heightIn(max = 260.dp)
                    ) {
                        items(presets) { (w, h, labelRes) ->
                            val isSelected = !isCustomMode &&
                                    selectedWidth == w &&
                                    selectedHeight == h

                            OutlinedButton(
                                onClick = {
                                    selectedWidth = w
                                    selectedHeight = h
                                    customWidthText = w.toString()
                                    customHeightText = h.toString()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = if (isSelected) {
                                    ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                } else {
                                    ButtonDefaults.outlinedButtonColors()
                                }
                            ) {
                                Text(
                                    text = stringResource(labelRes),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    // 自定义数值输入
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = customWidthText,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) customWidthText = input
                            },
                            label = { Text(stringResource(R.string.label_width)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = customHeightText,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) customHeightText = input
                            },
                            label = { Text(stringResource(R.string.label_height)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = stringResource(R.string.hint_even_number_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalWidth: Int
                    val finalHeight: Int

                    if (isCustomMode) {
                        val parsedW = customWidthText.toIntOrNull() ?: 1080
                        val parsedH = customHeightText.toIntOrNull() ?: 1920
                        // 偶数安全计算
                        finalWidth = ((parsedW.coerceAtLeast(2)) / 2) * 2
                        finalHeight = ((parsedH.coerceAtLeast(2)) / 2) * 2
                    } else {
                        finalWidth = selectedWidth
                        finalHeight = selectedHeight
                    }

                    onResolutionSelected(finalWidth, finalHeight)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.btn_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}


@Composable
fun SingleSelectOptionModal(
    title: String,
    options: List<String>,
    currentValue: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.heightIn(max = 280.dp)
            ) {
                items(options) { option ->
                    val isSelected = option.equals(currentValue, ignoreCase = true)
                    OutlinedButton(
                        onClick = {
                            onOptionSelected(option)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (isSelected) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        }
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
