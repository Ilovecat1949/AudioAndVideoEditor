package com.example.audioandvideoeditor.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.utils.LogUtils

@Composable
fun LogDisplayScreen() {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf<String?>(null) }
    val logDates = remember { mutableStateListOf<String>() }
    val logs by remember(selectedDate) {
        mutableStateOf(selectedDate?.let { LogUtils.getLogsByDate(context, it) } ?: "")
    }

    LaunchedEffect(Unit) {
        logDates.addAll(LogUtils.getAllLogDates(context))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (logDates.isEmpty()) {
            // Display message when there are no logs
            Text(
                text = "No errors or crashes have occurred.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(logDates) { date ->
                    Text(
                        text = date,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDate = date }
                            .padding(16.dp)
                            .background(if (selectedDate == date) Color.LightGray else Color.Transparent)
                    )
                    Divider()
                }
            }
        }
        if (selectedDate != null) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Logs for $selectedDate:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
                Text(
                    text = logs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("logs", logs)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Copy Logs")
                }
                Button(
                    onClick = {
                        LogUtils.clearLogs(context)
                        logDates.clear()
                        logDates.addAll(LogUtils.getAllLogDates(context))
                        selectedDate = null
                        Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Text("Clear Logs")
                }
            }
        }
    }
}

@Composable
fun LogDisplayScreen2() {
    val context = LocalContext.current
    var log_text by remember {
        mutableStateOf("")
    }
    LaunchedEffect(Unit) {
        log_text=LogUtils.getLogContext(context)
    }
    Column(modifier = Modifier.fillMaxSize()) {
        if (log_text.isEmpty()) {
            // Display message when there are no logs
            Text(
                text = "No errors or crashes have occurred.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)){
                item {
                    Text(log_text)
                }
            }
            Row(
               modifier = Modifier
                   .fillMaxWidth()
                   //.background(color = Color.Red)
                ,
               verticalAlignment = Alignment.CenterVertically
            ){
                Row(
                    horizontalArrangement = Arrangement.Start
                ){
                    Spacer(modifier = Modifier.width(20.dp))
                    Button(
                        onClick = {
                            LogUtils.clearLogs(context)
                            Toast.makeText(context, context.getString(R.string.clear_tip), Toast.LENGTH_SHORT).show()
                            log_text=""
                        }
                    ) {
                        Text(stringResource(id = R.string.clear))
                    }
                }
                Row (
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ){
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("logs", log_text)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, context.getString(R.string.copy_to_clipboard), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.padding(
                            end = 20.dp
                        )
                    ) {
                        Text(stringResource(id = R.string.copy))
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                }

            }
        }
    }
}