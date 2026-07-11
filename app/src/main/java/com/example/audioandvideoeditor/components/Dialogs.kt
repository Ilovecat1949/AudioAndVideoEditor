package com.example.audioandvideoeditor.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.utils.FilesUtils
import com.example.audioandvideoeditor.utils.LogUtils
import com.example.audioandvideoeditor.viewmodel.HomeViewModel

/**
 * 崩溃日志弹窗
 */
@Composable
fun CrashMessageDialog(viewModel: HomeViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val message = LogUtils.getLogContext(context)
    val scrollState = remember { ScrollState(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.crash_message_title)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.feedback_text2))
                Spacer(modifier = Modifier.height(20.dp))

                ContactInfoSection()

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("logs", message)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, context.getString(R.string.copy_to_clipboard), Toast.LENGTH_SHORT)
                                .apply { setGravity(android.view.Gravity.CENTER, 0, 0) }
                                .show()
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_content_copy_24),
                                contentDescription = stringResource(id = R.string.copy_to_clipboard)
                            )
                        }
                    }

                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        item { Text(message) }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {}
    )
}

/**
 * 版本更新弹窗
 */
@Composable
fun UpdateDialog(viewModel: HomeViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
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
                Text(stringResource(id = R.string.updates_tip2))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    ContactItem(
                        labelRes = R.string.releases_link,
                        valueRes = R.string.releases_link,
                        isClickable = true,
                        onClick = { FilesUtils.openWebLink(context, context.getString(R.string.releases_link)) }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    ContactItem(
                        labelRes = R.string.lanzout_link,
                        valueRes = R.string.lanzout_link,
                        isClickable = true,
                        onClick = { FilesUtils.openWebLink(context, context.getString(R.string.lanzout_link)) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {}
    )
}

/**
 * 屏幕内广告弹窗
 */
@Composable
fun OnScreenAdDialog(viewModel: HomeViewModel, onDismiss: () -> Unit, onDontRemindAgain: () -> Unit) {
    val context = LocalContext.current
    val scrollState = remember { ScrollState(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.welcome)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.welcome_text))
                Spacer(modifier = Modifier.height(20.dp))
                ContactInfoSection()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                ConfigsUtils.setShowOnScreenAdAgainFlag(context, false)
            }) {
                Text(stringResource(id = R.string.dont_remind_again))
            }
        }
    )
}