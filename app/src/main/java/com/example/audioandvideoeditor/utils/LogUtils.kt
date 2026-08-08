package com.example.audioandvideoeditor.utils
import android.content.Context
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.StringRes
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
object LogUtils {

    private const val LOG_FILE_NAME = "app_error_logs.log"

    fun log(context: Context, tag: String, message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val logEntry = "$date $timestamp [$tag]: $message\n"
        synchronized(this) {
            try {
                val logFile = getLogFile(context)
                val writer = FileWriter(logFile, true)
                writer.append(logEntry)
                writer.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun getLogFile(context: Context): File {
        return File(context.filesDir, LOG_FILE_NAME)
    }
    fun getLogContext(context: Context):String{
        val logFile = getLogFile(context)
        if (!logFile.exists()) return ""
        return try {
            logFile.readText()
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }
    fun getLogsByDate(context: Context, date: String): String {
        val logFile = getLogFile(context)
        if (!logFile.exists()) return ""

        return try {
            val logs = logFile.readLines()
            logs.filter { it.startsWith(date) }.joinToString("\n")
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }

    fun getAllLogDates(context: Context): List<String> {
        val logFile = getLogFile(context)
        if (!logFile.exists()) return emptyList()

        return try {
            val logs = logFile.readLines()
            logs.mapNotNull { it.substringBefore(" ").takeIf { it.isNotEmpty() } }.distinct()
        } catch (e: IOException) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun clearLogs(context: Context) {
        val logFile = getLogFile(context)
        if (logFile.exists()) {
            logFile.delete()
        }
    }

    /**
     * 带有居中样式的短 Toast
     */
    fun Context.showToast(message: String, isShort: Boolean = true) {
        val duration = if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
        // 直接 makeText 并 show，无需且不要设置 setGravity
        Toast.makeText(this, message, duration).show()
    }

    /**
     * 接受 StringRes 的重载
     */
    fun Context.showToast(@StringRes resId: Int, isShort: Boolean = true) {
        showToast(getString(resId), isShort)
    }
}