package com.example.audioandvideoeditor.components

import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.utils.FilesUtils
import com.example.audioandvideoeditor.viewmodel.FilesViewModel
import java.io.File

@Composable
fun FilesListScreen(
    filesViewModel: FilesViewModel = viewModel()
){
    val life= rememberLifecycle()
    life.onLifeCreate {
        if(filesViewModel.parent==null){
            val file= File(Environment.getExternalStorageDirectory().path)
            file.listFiles()?.let {
                filesViewModel.filesList.addAll(it)
            }
            filesViewModel.parent= file
        }
    }
    FilesList(filesViewModel)
}

@Composable
private fun FilesList(
    filesViewModel: FilesViewModel
){
    Row(
        modifier = Modifier
            .padding(top = 20.dp, start = 20.dp)
            .height(40.dp)
            .fillMaxWidth()
            .clickable {
                if (filesViewModel.parent != null && filesViewModel.parent!!.path != Environment.getExternalStorageDirectory().path) {
                    if (filesViewModel.parent!!.parentFile != null) {
                        filesViewModel.parent!!.parentFile
                            ?.listFiles()
                            ?.let {
                                filesViewModel.filesList.clear()
                                filesViewModel.filesList.addAll(it)
                                //filesViewModel.initFilesState()
                                filesViewModel.parent = filesViewModel.parent!!.parentFile
                            }
                    } else {
                        filesViewModel.filesList.clear()
                        filesViewModel.filesList.add(filesViewModel.parent!!)
                        //filesViewModel.initFilesState()
                        filesViewModel.parent = null
                    }

                }
            }
    ){
        Icon(painter = painterResource(id = R.drawable.baseline_folder_24), contentDescription = null)
        Spacer(modifier = Modifier.width(40.dp))
        Text(text="..")
    }
    LazyColumn(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp)
    ){
        items(count =filesViewModel.filesList.size){
            ShowFile(filesViewModel,it)
        }
    }
}
@Composable
private fun ShowFile(
    filesViewModel: FilesViewModel,
    id:Int
){
    val file=filesViewModel.filesList[id]
    if(!filesViewModel.filesState.containsKey(file.path)){
        filesViewModel.filesState[file.path]= mutableStateOf(false)
    }
    val ctx=LocalContext.current
    if(file.isFile){
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_insert_drive_file_24),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(20.dp))
            if(file.name.length<20) {
                Text(text = file.name)
            }
            else{
                Text(text =file.name.substring(0,14)+"..."+file.name.substring(file.name.length-5))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ){
                TextButton(onClick = {
                    FilesUtils.copyStr(file.path, ctx)
                }) {
                    Text(text="复制路径")
                }
            }
        }
    }
    else if(file.isDirectory){
        Row(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth()
                .clickable {
                    val dir = file
                    if (dir.listFiles() != null) {
                        filesViewModel.filesList.clear()
                        filesViewModel.parent = dir
                        dir
                            .listFiles()
                            ?.let { it1 ->
                                filesViewModel.filesList.addAll(it1)
                                filesViewModel.initFilesState()
                            }
                    }
                }
        ){
            Icon(painter = painterResource(id = R.drawable.baseline_folder_24), contentDescription = null)
            Spacer(modifier = Modifier.width(20.dp))
            Text(text=file.name)
            Spacer(modifier = Modifier.width(10.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ){
                TextButton(onClick = {
                    FilesUtils.copyStr(file.path, ctx)
                }) {
                    Text(text="复制路径")
                }
            }
        }
    }
}