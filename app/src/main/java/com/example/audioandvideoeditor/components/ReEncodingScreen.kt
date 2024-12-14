package com.example.audioandvideoeditor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audioandvideoeditor.MainActivity
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.entity.TaskInfo
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.viewmodel.ReEncodingViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun ReEncodingScreen(
    activity: MainActivity,
    file:File,
    nextDestination:()->Unit,
    reEncodingViewModel:ReEncodingViewModel= viewModel()
){
    val life= rememberLifecycle()
    val context=LocalContext.current
    life.onLifeCreate {
        reEncodingViewModel.mediaInfo.initInfo(activity.tasksBinder.getAVInfo(file.path))
        reEncodingViewModel.bit_rate=-1L
        reEncodingViewModel.frame_rate=-1
        val infoMap=reEncodingViewModel.mediaInfo.infoMap
        if(infoMap.contains("VideoBitRate")){
            val kbit_rate=infoMap["VideoBitRate"]!!.toLong()/1000
            reEncodingViewModel.bit_rate_text.value="${context.getString(R.string.bitrate)}:${kbit_rate}kb/s"
        }
        else{
            reEncodingViewModel.bit_rate_text.value="${context.getString(R.string.bitrate)}:${context.getString(R.string.unknown)}"
        }
        if(infoMap.contains("FrameRate")){
            reEncodingViewModel.frame_rate_text.value="${context.getString(R.string.frame_rate)}:${infoMap["FrameRate"]!!.toFloat().toInt()}"
        }
        else{
            reEncodingViewModel.frame_rate_text.value="${context.getString(R.string.frame_rate)}:${context.getString(R.string.unknown)}"
        }
    }
    if(reEncodingViewModel.input_file_name.value.length==0){
        val date= Date(System.currentTimeMillis())
        val formatter=SimpleDateFormat("yyyyMMddHHmmss", LocalContext.current.resources.configuration.locales[0])
        reEncodingViewModel.input_file_name.value= LocalContext.current.getString(R.string.reencoding)+formatter.format(date)
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
        ReEncodingScreen2(
            activity, file, nextDestination, reEncodingViewModel
        )
    }
    showEditBitRateScreen(reEncodingViewModel)
    showFrameRateScreen(reEncodingViewModel )
}
@Composable
private fun ReEncodingScreen2(
    activity: MainActivity,
    file:File,
    nextDestination:()->Unit,
    reEncodingViewModel:ReEncodingViewModel
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
                value = reEncodingViewModel.input_file_name.value,
                onValueChange = {
                    reEncodingViewModel.input_file_name.value = it
                },
                label = { Text(LocalContext.current.resources.getString(R.string.file_title)) }
            )
            Spacer(modifier = Modifier.width(5.dp))
            TypesMenu(
                modifier =Modifier.width(120.dp),
                reEncodingViewModel = reEncodingViewModel
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = {
            startReEncoding(activity, file, nextDestination, reEncodingViewModel)
        }) {
            Text(text = LocalContext.current.resources.getString(R.string.reencoding))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = {
            reEncodingViewModel.editBitRateFlag.value=true
        }) {
            Text(text=reEncodingViewModel.bit_rate_text.value)
        }
        Button(onClick = {
            reEncodingViewModel.editFrameRateFlag.value=true
        }) {
            Text(text =reEncodingViewModel.frame_rate_text.value)
        }
    }
}
private  fun startReEncoding(
    activity: MainActivity,
    file:File,
    nextDestination:()->Unit,
    reEncodingViewModel:ReEncodingViewModel
){
    val int_arr=ArrayList<Int>()
    int_arr.add(0)
    int_arr.add(reEncodingViewModel.frame_rate)
    val long_arr=ArrayList<Long>()
    long_arr.add(reEncodingViewModel.bit_rate*1000)
    long_arr.add(ConfigsUtils.sizeForVideoEncodingTask)
    long_arr.add(ConfigsUtils.sizeForAudioEncodingTask)
    val str_arr=ArrayList<String>()
    val targetPath=ConfigsUtils.target_dir+"/"+reEncodingViewModel.input_file_name.value+"."+reEncodingViewModel.selectedOptionText.value
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
    reEncodingViewModel.input_file_name.value=""
    nextDestination()
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypesMenu(
    modifier: Modifier,
    reEncodingViewModel:ReEncodingViewModel
){
    val options = reEncodingViewModel.options
    val expanded =reEncodingViewModel.expanded
    val selectedOptionText =reEncodingViewModel.selectedOptionText
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

@Composable
private fun showEditBitRateScreen(
    reEncodingViewModel: ReEncodingViewModel
){
    var input_text by remember {
        mutableStateOf("")
    }
    if(reEncodingViewModel.editBitRateFlag.value) {
        AlertDialog(
            onDismissRequest = { reEncodingViewModel.editBitRateFlag.value = false },
            title = {
                Text(text = LocalContext.current.resources.getString(R.string.set_bitrate))
            },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
//                        .height(40.dp)
                        .fillMaxWidth()
                ){
                    TextField(
                        modifier = Modifier.width(200.dp),
                        value = input_text,
                        onValueChange = {
                            if(it.isDigitsOnly()){
                                input_text=it
                            }
                        },
                        label = { Text(LocalContext.current.getString(R.string.bitrate)) },
//                    visualTransformation = ,
//                    keyboardOptions = KeyboardType.Number
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text="kb/s")
                }

            },
            confirmButton = {
                val ctx=LocalContext.current
                TextButton(
                    onClick = {
                        if(input_text.isDigitsOnly()){
                            reEncodingViewModel.bit_rate=input_text.toLong()
                            reEncodingViewModel.bit_rate_text.value="${ctx.getString(R.string.bitrate)}:${input_text}kb/s"
                        }
                        reEncodingViewModel.editBitRateFlag.value = false
                    }
                ) {
                    Text(LocalContext.current.getString(R.string.determine))
                }

            },
            dismissButton = {
                TextButton(
                    onClick = {
                        reEncodingViewModel.editBitRateFlag.value = false
                    }
                ) {
                    Text(LocalContext.current.getString(R.string.cancel))
                }
            })
    }
}
@Composable
private fun showFrameRateScreen(
    reEncodingViewModel: ReEncodingViewModel
){
    var input_text by remember {
        mutableStateOf("")
    }
    if(reEncodingViewModel.editFrameRateFlag.value) {
        AlertDialog(
            onDismissRequest = { reEncodingViewModel.editFrameRateFlag.value = false },
            title = {
                Text(text = LocalContext.current.getString(R.string.set_frame_rate))
            },
            text = {
                TextField(
                    modifier = Modifier.width(200.dp),
                    value = input_text,
                    onValueChange = {
                        if(it.isDigitsOnly()){
                            input_text=it
                        }
                    },
                    label = { Text(LocalContext.current.getString(R.string.frame_rate)) },
//                    visualTransformation = ,
//                    keyboardOptions = KeyboardType.Number
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if(input_text.isDigitsOnly()){
                            reEncodingViewModel.frame_rate=input_text.toInt()
                            reEncodingViewModel.frame_rate_text.value="编码帧率:${input_text}"
                        }
                        reEncodingViewModel.editFrameRateFlag.value = false
                    }
                ) {
                    Text(LocalContext.current.getString(R.string.determine))
                }

            },
            dismissButton = {
                TextButton(
                    onClick = {
                        reEncodingViewModel.editFrameRateFlag.value = false
                    }
                ) {
                    Text(LocalContext.current.getString(R.string.cancel))
                }
            })
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdownMenuSample() {
    val options = listOf("Option 1", "Option 2", "Option 3", "Option 4", "Option 5")
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionText by remember { mutableStateOf(options[0]) }
    // We want to react on tap/press on TextField to show menu
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        TextField(
            readOnly = true,
            value = selectedOptionText,
            onValueChange = { },
            label = { Text("Label") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            modifier = Modifier.menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = {
                        Text(selectionOption)
                    },
                    onClick = {
                        selectedOptionText = selectionOption
                        expanded = false
                    }
                )
            }
        }
    }
}
