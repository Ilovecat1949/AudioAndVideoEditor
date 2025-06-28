package com.example.audioandvideoeditor.viewmodel

import android.os.Environment
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class ConfigViewModel2: ViewModel()  {
    // Download Path
    private val _downloadPath = mutableStateOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path)
    val downloadPath: State<String> = _downloadPath

    // Language Management (Using Pair)
    private val _availableLanguages = mutableStateOf(listOf(
        Pair("en", "English"),
        Pair("zh", "简体中文")
    ))
    val availableLanguages: State<List<Pair<String, String>>> = _availableLanguages

    private val _appLanguage = mutableStateOf(getCurrentAppLanguage())
    val appLanguage: State<Pair<String, String>> = _appLanguage

    private fun getCurrentAppLanguage(): Pair<String, String> {
        val currentLocale = Locale.getDefault()
        val langCode = currentLocale.language
        return _availableLanguages.value.find { it.first == langCode } ?: _availableLanguages.value[0]
    }

    fun setAppLanguage(language: Pair<String, String>) {
        _appLanguage.value = language
        // Save to SharedPreferences (Not implemented here)
    }

    // Text Content (From resources)
    val privacyPolicyText = "Your privacy policy text goes here.  It can be quite long." // Replace with string resource
    val appInfoText = "App Name: My App\nVersion: 1.0.0"  // Replace with string resource
    val authorInfoText = "Author: Your Name" // Replace with string resource

    // Error Handling
    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    fun updateDownloadPath(newPath: String) {
        viewModelScope.launch {
            try {
                val file = File(newPath)
                if (!file.exists() && !file.mkdirs()) {
                    throw Exception("Failed to create directory.")
                }
                _downloadPath.value = newPath
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error"
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}