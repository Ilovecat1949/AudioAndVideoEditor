package com.example.audioandvideoeditor.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.audioandvideoeditor.R

object PermissionsUtils {
    var areSelfExternalStoragePermissionEnabled = true
        private set
    fun checkSelfExternalStoragePermission(activity: Activity){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
            val REQUEST_CODE_CONTACT = 101
            val permission = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            permission.forEach {
                if (activity.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED) {
                    activity.requestPermissions(permission, REQUEST_CODE_CONTACT)
                    areSelfExternalStoragePermissionEnabled= false
                } else {
                    if (it.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
//                        Log.d("PermissionManager","读权限")
                        Toast.makeText(
                            activity,
                            "已获得访问Android文件读权限",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    if (it.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        Toast.makeText(
                            activity,
                            "已获得访问Android文件写权限",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    areSelfExternalStoragePermissionEnabled=true
                }
            }
        }
        else{
            if(Environment.isExternalStorageManager()){
                Toast.makeText(
                    activity,
                    "已获得访问Android所有文件权限",
                    Toast.LENGTH_SHORT
                ).show()
                areSelfExternalStoragePermissionEnabled=true
            }
            else{
                areSelfExternalStoragePermissionEnabled=false
                val builder = AlertDialog.Builder(activity)
                    .setMessage(activity.getString(R.string.request_file_permissions))
                    .setPositiveButton(activity.getString(R.string.determine)) { _, _ ->
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        ContextCompat.startActivity(activity, intent, null)
                    }
                builder.show()
            }
        }
    }
    var areNotificationsEnabled=true
        private set
    fun checkNotificationsEnabled(activity: Activity){
        val manager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 检测该应用是否有通知权限
        areNotificationsEnabled=manager.areNotificationsEnabled()
        if(!areNotificationsEnabled && Build.VERSION.SDK_INT>= Build.VERSION_CODES.TIRAMISU){
            activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 102)
        }
    }
}