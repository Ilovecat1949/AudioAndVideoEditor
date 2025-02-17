package com.example.audioandvideoeditor.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

object FilesUtils {
    fun getThumbnail(contentResolver: ContentResolver, uri: Uri): ImageBitmap?{
        try {
            return if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.Q) {
                contentResolver.loadThumbnail(
                    uri, Size(640, 480), null
                ).asImageBitmap()
            } else{
                null
            }
        }
        catch (e:Exception){
            e.printStackTrace()
        }
        return null
    }
    fun getNameFromPath(path:String):String{
        var i=path.length-1
        while(i>-1){
            if(path[i]=='/'){
                break
            }
            i--
        }
        if(i>=0 && i<path.length-1){
            return path.substring(i+1)
        }
        return ""
    }
    fun getTypeFromPath(path:String):String{
        var i=path.length-1
        while(i>-1){
            if(path[i]=='.'){
                break
            }
            i--
        }
        if(i>=0 && i<path.length-1){
            return path.substring(i+1)
        }
        return ""
    }
    fun copyStr(copyStr:String,ctx:Context):Boolean{
        return try {
            val cm=ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val mClipData = ClipData.newPlainText("Label", copyStr)
            cm.setPrimaryClip(mClipData)
            true
        } catch (e:Exception){
            false
        }
    }
}