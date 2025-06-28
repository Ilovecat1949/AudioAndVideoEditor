package com.example.audioandvideoeditor.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.viewmodel.ConfigViewModel2

// Reusable Error Dialog
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

// Screens (Privacy Policy, App Info, Author Info) - Structure is the same for all
// Screens (Privacy Policy, App Info, Author Info) - Structure is the same for all
@Composable
private fun TextScreen(title: String, text: String) {
    Scaffold(
        topBar = {
//            TopAppBar(
//                title = { Text(title) },
//                navigationIcon = {
//                    IconButton(onClick = { navController.navigateUp() }) {
//                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                Text(text)
            }
        }
    )
}

@Composable
private fun SettingsScreen(viewModel: ConfigViewModel2, nextDestination: (route: String) -> Unit) {
    val context = LocalContext.current
    val downloadPath = viewModel.downloadPath.value
    var showPathDialog by remember { mutableStateOf(false) }
    var newDownloadPath by remember { mutableStateOf(downloadPath) }
    val availableLanguages = viewModel.availableLanguages.value
    val appLanguage = viewModel.appLanguage.value
    val errorMessage = viewModel.errorMessage.value

    Scaffold(
        topBar = {
//            TopAppBar(title = { Text(stringResource(R.string.settings_title)) },
//                navigationIcon = {
//                    IconButton(onClick = { /* Handle back navigation as needed */ }) {
//                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
//                    }
//                })
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Download Path
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.target_dir), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(downloadPath, modifier = Modifier.clickable { showPathDialog = true })
                }
                if (showPathDialog) {
                    AlertDialog(
                        onDismissRequest = { showPathDialog = false },
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
                                viewModel.updateDownloadPath(newDownloadPath)
                                if (errorMessage == null) {
                                    showPathDialog = false
                                }
                            }) {
                                Text(stringResource(id = R.string.ok))
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showPathDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }
                // Language
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.switch_language), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Text(appLanguage.second, modifier = Modifier.clickable { expanded = true })
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            availableLanguages.forEach { language ->
                                DropdownMenuItem(onClick = {
                                    viewModel.setAppLanguage(language)
                                },
                                text={
                                    Text(language.second)
                                }
                                )
                            }
                        }
                    }
                }
                // Navigation Links
                Text(text= stringResource(id = R.string.privacy_policy),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        //openPrivacyPolicy(context)
                    })
                Text(text= stringResource(id = R.string.app_info),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {  })
                Text("作者信息",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {  })
                errorMessage?.let { ErrorDialog(it) { viewModel.clearErrorMessage() } }
            }
        })

}


@Composable
fun ConfigScreen2(
    viewModel: ConfigViewModel2= androidx.lifecycle.viewmodel.compose.viewModel(),
    ){
    SettingsScreen(
        viewModel,
        {}
    )
}