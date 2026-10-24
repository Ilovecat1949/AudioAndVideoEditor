package com.example.audioandvideoeditor.ui.ffmpegcommand

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.utils.FFmpegTemplate
import java.io.File
import java.util.UUID

class FFmpegCommandsViewModel: ViewModel()  {
    val command_args_str = mutableStateOf("")
    val command_args = mutableStateListOf<MutableState<String>>()
    val show_log_flag = mutableStateOf(false)
    var task_flag = 0
    val log_lines = mutableStateListOf<String>()
    var task_log_path = ""

    // 🌟 多输入文件列表（统一管理单选/多选）
    val selectedInputFiles = mutableStateListOf<File>()

    var output_file_name by mutableStateOf("")
    val extensionTemplate = listOf("mp4", "mkv", "mov", "flv", "ts", "avi", "mp3", "m4a")
    var extensionText by mutableStateOf(extensionTemplate[0])

    // 模板管理相关状态
    val templates = mutableStateListOf<FFmpegTemplate>()
    var selectedTemplate by mutableStateOf<FFmpegTemplate?>(null)

    var parameterNameText by mutableStateOf("")
    var parameterContextText by mutableStateOf("")

    /**
     * 更新选中的文件列表
     */
    fun updateInputFiles(files: List<File>?) {
        if (!files.isNullOrEmpty()) {
            selectedInputFiles.clear()
            selectedInputFiles.addAll(files)
        }
    }

    /**
     * 移除单个文件
     */
    fun removeInputFile(file: File) {
        selectedInputFiles.remove(file)
    }

    /**
     * 清空文件
     */
    fun clearInputFiles() {
        selectedInputFiles.clear()
    }

    /**
     * 初始化加载模板（合并系统内置与本地持久化的自定义模板）
     */
    fun initTemplates(context: Context, defaultNames: List<String>, defaultContexts: List<String>) {
        if (templates.isNotEmpty()) return

        val loadedCustom = ConfigsUtils.loadCustomTemplates(context).sortedByDescending { it.createdAt }

        val builtIns = defaultNames.indices.map { i ->
            FFmpegTemplate(
                id = "builtin_$i",
                name = defaultNames[i],
                context = defaultContexts[i],
                isBuiltIn = true,
                createdAt = 0L
            )
        }

        templates.clear()
        templates.addAll(loadedCustom)
        templates.addAll(builtIns)

        if (templates.isNotEmpty() && selectedTemplate == null) {
            selectedTemplate = templates[0]
            parameterNameText = templates[0].name
            parameterContextText = templates[0].context
        }
    }

    fun addCustomTemplate(context: Context, name: String, paramContext: String) {
        val newTemplate = FFmpegTemplate(
            id = UUID.randomUUID().toString(),
            name = name,
            context = paramContext,
            isBuiltIn = false,
            createdAt = System.currentTimeMillis()
        )
        val insertIndex = templates.indexOfFirst { it.isBuiltIn }
        if (insertIndex != -1) {
            templates.add(insertIndex, newTemplate)
        } else {
            templates.add(0, newTemplate)
        }
        ConfigsUtils.saveCustomTemplates(context, templates)
        selectTemplate(newTemplate)
    }

    fun updateCustomTemplate(context: Context, templateId: String, newName: String, newContext: String) {
        val index = templates.indexOfFirst { it.id == templateId }
        if (index != -1 && !templates[index].isBuiltIn) {
            val updated = templates[index].copy(
                name = newName,
                context = newContext,
                createdAt = System.currentTimeMillis()
            )
            templates[index] = updated
            reorderTemplates()
            ConfigsUtils.saveCustomTemplates(context, templates)
            if (selectedTemplate?.id == templateId) {
                selectTemplate(updated)
            }
        }
    }

    fun deleteCustomTemplate(context: Context, templateId: String) {
        val index = templates.indexOfFirst { it.id == templateId }
        if (index != -1 && !templates[index].isBuiltIn) {
            templates.removeAt(index)
            ConfigsUtils.saveCustomTemplates(context, templates)
            if (selectedTemplate?.id == templateId && templates.isNotEmpty()) {
                selectTemplate(templates[0])
            }
        }
    }

    fun selectTemplate(template: FFmpegTemplate) {
        selectedTemplate = template
        parameterNameText = template.name
        parameterContextText = template.context
    }

    private fun reorderTemplates() {
        val custom = templates.filter { !it.isBuiltIn }.sortedByDescending { it.createdAt }
        val builtIn = templates.filter { it.isBuiltIn }
        templates.clear()
        templates.addAll(custom)
        templates.addAll(builtIn)
    }
}