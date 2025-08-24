package com.example.audioandvideoeditor.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("name") val name: String,
    @SerialName("body") val body: String,
    @SerialName("html_url") val htmlUrl: String,
    val assets: List<GitHubAsset>
)

@Serializable
data class GitHubAsset(
    val name: String,
    val size: Long,
    @SerialName("browser_download_url") val browserDownloadUrl: String
)

object ConfigsUtils {

    var target_dir= Environment.getExternalStorageDirectory().path + "/Download"
        private set
    var AV_10MB=10*1024*1024L
        private set
    var AV_50MB=50*1024*1024L
        private set
    var AV_100MB=100*1024*1024L
        private set
    var AV_500MB=500*1024*1024L
        private set
    var AV_1GB=1024*1024*1024L
        private set
    var AV_2GB=2*1024*1024*1024L
        private set
    var AV_3GB=3*1024*1024*1024L
        private set
    var sizeForVideoEncodingTask=AV_50MB
        private set
    var sizeForAudioEncodingTask=AV_50MB
        private set
    var MAX_TASKS_NUM=1
        private set
    val English="english"
    val Simplified_Chinese="simplified_chinese"
    var language=Simplified_Chinese
        private set
    var show_crash_message_flag=false
    private set
    var show_on_screen_ad_again_flag=true
        private set
    var files_sort_flag=0
    private set
//    var notificationsRemind=true
//        private set
//    var externalStoragePermissionRemind=true
//        private set
    fun InitConfig(context: Context){
        val editor = context.getSharedPreferences("data", Context.MODE_PRIVATE).edit()
        val prefs = context.getSharedPreferences("data", Context.MODE_PRIVATE)
        var target_dir_name=prefs.getString("target_dir","")!!
        if(target_dir_name.length==0){
            target_dir_name=target_dir
            editor.putString("target_dir",target_dir)
        }
        else{
            target_dir= target_dir_name
        }
        var size=prefs.getLong("sizeForVideoEncodingTask",-1)
        if(size<0){
            size=AV_50MB
            editor.putLong("sizeForVideoEncodingTask",size)
        }
        sizeForVideoEncodingTask=size
        size=prefs.getLong("sizeForAudioEncodingTask",-1)
        if(size<0){
            size=AV_50MB
            editor.putLong("sizeForAudioEncodingTask",size)
        }
        sizeForAudioEncodingTask=size
        MAX_TASKS_NUM=prefs.getInt("MAX_TASKS_NUM",-1)
        if(MAX_TASKS_NUM<0){
            MAX_TASKS_NUM=1
            editor.putInt("MAX_TASKS_NUM",MAX_TASKS_NUM)
        }
        language=prefs.getString("language","")!!
        if(language.length==0){
            language=Simplified_Chinese
            editor.putString("language",language)
        }
        show_crash_message_flag=prefs.getBoolean("show_crash_message_flag",false)
        show_on_screen_ad_again_flag=prefs.getBoolean("show_on_screen_ad_again_flag",true)
        files_sort_flag=prefs.getInt("files_sort_flag",1)
//        notificationsRemind=prefs.getBoolean("notificationsRemind",true)
//        externalStoragePermissionRemind=prefs.getBoolean("externalStoragePermissionRemind",true)
        editor.apply()
    }
    fun setSizeForVideoEncodingTask(size:Long,activity: Activity){
        val editor = activity.getSharedPreferences("data", Context.MODE_PRIVATE).edit()
        sizeForVideoEncodingTask=size
        editor.putLong("sizeForVideoEncodingTask",size)
        editor.apply()
    }
    fun setSizeForAudioEncodingTask(size:Long,activity: Activity){
        val editor = activity.getSharedPreferences("data", Context.MODE_PRIVATE).edit()
        sizeForAudioEncodingTask=size
        editor.putLong("sizeForAudioEncodingTask",size)
        editor.apply()
    }
    fun setMaxTasksNum(size:Int,activity: Activity){
        val editor = activity.getSharedPreferences("data", Context.MODE_PRIVATE).edit()
        MAX_TASKS_NUM=size
        editor.putInt("MAX_TASKS_NUM",MAX_TASKS_NUM)
        editor.apply()
    }
    fun setLanguage(new_language:String,activity: Activity){
        val editor = activity.getSharedPreferences("data", Context.MODE_PRIVATE).edit()
        language=new_language
        editor.putString("language",new_language)
        editor.apply()
    }
    fun setTargetDir(new_target_dir_name:String,activity: Activity){
        val editor = activity.getSharedPreferences("data", Context.MODE_PRIVATE).edit()
        target_dir=new_target_dir_name
        editor.putString("target_dir",target_dir)
        editor.apply()
    }
    private fun getLocaleLanguage():Locale{
        return when(language){
            English-> Locale.ENGLISH
            Simplified_Chinese->Locale.SIMPLIFIED_CHINESE
            else -> Locale.SIMPLIFIED_CHINESE
        }
    }
    fun setCurrLanguageMode(context: Context?):Context?{
        if(context!=null){
            val local = getLocaleLanguage()
            val res = context.applicationContext.resources
            val conf = res.configuration
            conf.setLocale(local)
            return context.createConfigurationContext(conf)
        }
        return context
    }
    fun setCrashMessageFlag(context: Context,flag:Boolean){
        val editor = context.getSharedPreferences("data", Context.MODE_PRIVATE).edit()
        editor.putBoolean("show_crash_message_flag",flag)
        editor.apply()
        show_crash_message_flag=flag
    }
    fun setShowOnScreenAdAgainFlag(context: Context,flag:Boolean){
        val editor = context.getSharedPreferences("data", Context.MODE_PRIVATE).edit()
        editor.putBoolean("show_on_screen_ad_again_flag",flag)
        editor.apply()
        show_on_screen_ad_again_flag=flag
    }
    fun setFilesSortFlag(context: Context,flag:Int){
        val editor = context.getSharedPreferences("data", Context.MODE_PRIVATE).edit()
        editor.putInt("files_sort_flag",flag)
        editor.apply()
        files_sort_flag=flag
    }

//    fun setPermissionRemind(context: Context,remindFlag:Boolean,permission:Int){
//        val editor = context.getSharedPreferences("data", Context.MODE_PRIVATE).edit()
//        when(permission){
//          0->  {
//              editor.putBoolean("externalStoragePermissionRemind",remindFlag)
//              externalStoragePermissionRemind=remindFlag
//          }
//          1->  {
//              editor.putBoolean("notificationsRemind",remindFlag)
//              notificationsRemind=remindFlag
//          }
//        }
//        editor.apply()
//    }
// 创建一个 Json 实例
private val json = Json { ignoreUnknownKeys = true }

var gitHubRelease by mutableStateOf<GitHubRelease?>(null)

    /**
     * 比较版本号，格式为 "vX.Y.Z"
     * @return 如果最新版本号大于当前版本号，返回 true
     */
fun isNewVersionAvailable(current: String, latest: String): Boolean {
        val currentParts = current.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latest.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
            val currentPart = currentParts.getOrElse(i) { 0 }
            val latestPart = latestParts.getOrElse(i) { 0 }
            if (latestPart > currentPart) {
                return true
            }
            if (latestPart < currentPart) {
                return false
            }
        }
        return false // 版本号相同
    }
suspend fun getLatestGitHubRelease(owner: String, repo: String): GitHubRelease? = withContext(
        Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        var reader: BufferedReader? = null

        try {
            val url = URL("https://api.github.com/repos/$owner/$repo/releases/latest")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val stream = connection.inputStream
                reader = BufferedReader(InputStreamReader(stream))
                val response = reader.readText()
                // 使用 kotlinx.serialization 来解析 JSON 字符串
                json.decodeFromString<GitHubRelease>(response)
            } else {
                null
            }
        } catch (e: Exception) {
            // 如果解析失败，可能是网络问题或 JSON 格式不匹配
            if (e is SerializationException) {
                // 记录序列化错误
            }
            e.printStackTrace()
            null
        } finally {
            reader?.close()
            connection?.disconnect()
        }
    }
}