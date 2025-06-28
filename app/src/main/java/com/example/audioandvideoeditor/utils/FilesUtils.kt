package com.example.audioandvideoeditor.utils

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.util.Size
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileNotFoundException

object FilesUtils {
    fun getThumbnail(context:Context,contentResolver: ContentResolver, uri: Uri):Bitmap?{
        val retriever = MediaMetadataRetriever()
        return try {
            // 通过 ContentResolver 设置数据源
            retriever.setDataSource(context, uri)

            retriever.embeddedPicture?.let { embeddedPicture ->
                BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
            } ?: retriever.getFrameAtTime(
                 1_000_000,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }
    fun getThumbnail(contentResolver: ContentResolver, uri: Uri): ImageBitmap?{
        try {
            return if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.Q) {
                contentResolver.loadThumbnail(
                    uri, Size(640, 480), null
                ).asImageBitmap()//640, 480
            } else{
                null
            }
        }
        catch (e:Exception){
            e.printStackTrace()
        }
        return null
    }
    fun getThumbnail(path:String): ImageBitmap?{
        try {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ThumbnailUtils.createVideoThumbnail(File(path), Size(640, 480), null)
                    .asImageBitmap()
            } else {
                null
            }
        }
        catch (e:Exception){
            e.printStackTrace()
        }
        return null
    }
    fun getVideoCover(videoPath: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            // 设置数据源（支持本地路径或 URI）
            retriever.setDataSource(videoPath)

            // 优先尝试获取嵌入的专辑封面（返回 Bitmap）
            val embeddedPicture = retriever.embeddedPicture
            if (embeddedPicture != null) {
                BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
            } else {
                // 若无嵌入封面，提取第一帧作为封面
                retriever.getFrameAtTime(
                    1_000_000, // 从第 1ms 处开始（避免全黑帧）
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            retriever.release() // 确保释放资源
        }
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

    fun getMimeType(file: File): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(file.toURI().toString())
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
    }
    fun openLocalFile(context: Context, file: File) {
        if (!file.exists()) {
            // 文件不存在，可以给用户一个提示
            // Toast.makeText(context, "文件不存在", Toast.LENGTH_SHORT).show()
            return
        }
        val mimeType = getMimeType(file)
        // 使用 FileProvider 获取 content URI
        val uri: Uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider", // 替换为你在 AndroidManifest.xml 中定义的 authority
                file
            )
        } catch (e: IllegalArgumentException) {
            // 如果 FileProvider 配置不正确或文件路径不在 FileProvider 声明的范围内，可能会抛出此异常
            // 可以在这里处理错误，例如日志记录或给用户提示
            e.printStackTrace()
            // 作为备用，但可能在 Android 7.0+ 上引发 FileUriExposedException
            Uri.fromFile(file)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // 授予读取 URI 的临时权限给接收应用
            // 对于 Android 11+ (API 30+)，如果目标应用不是默认浏览器，
            // 且你的应用没有 QUERY_ALL_PACKAGES 权限，可能还需要添加 FLAG_ACTIVITY_NEW_TASK
            // 或者在 Manifest 中声明 <queries> 标签
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // 没有找到合适的应用来打开文件
            // Toast.makeText(context, "没有找到可以打开此文件的应用", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    // 打开网页链接
    fun openWebLink(context: Context, url: String) {
        val webpage: Uri? = try {
            Uri.parse(url)
        } catch (e: Exception) {
            // URL 格式错误
            // Toast.makeText(context, "无效的网页链接", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            null
        }

        webpage?.let {
            val intent = Intent(Intent.ACTION_VIEW, it).apply {
                // 通常打开网页链接不需要 NEW_TASK 标志，除非是从非 Activity context 启动
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // 没有找到浏览器应用
                // Toast.makeText(context, "没有找到浏览器应用", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
}