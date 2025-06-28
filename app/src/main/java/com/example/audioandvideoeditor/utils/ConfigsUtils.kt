package com.example.audioandvideoeditor.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import java.util.Locale

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
}