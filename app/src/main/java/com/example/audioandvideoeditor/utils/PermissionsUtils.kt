package com.example.audioandvideoeditor.utils

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat.startActivity
import com.example.audioandvideoeditor.R

object PermissionsUtils {
    var areSelfExternalStoragePermissionEnabled by mutableStateOf(true)
        private set
    fun requestSelfExternalStoragePermission(activity: ComponentActivity){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
            val permission = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            val requestPermissionLauncher =
                activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                    areSelfExternalStoragePermissionEnabled=isGranted
                }
            permission.forEach {
                areSelfExternalStoragePermissionEnabled = (activity.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED)
                if(!areSelfExternalStoragePermissionEnabled){
                    requestPermissionLauncher.launch(it)
                }
            }
        }
        else{
            areSelfExternalStoragePermissionEnabled = Environment.isExternalStorageManager()
            if(!areSelfExternalStoragePermissionEnabled){
                val builder = AlertDialog.Builder(activity)
                    .setMessage(activity.getString(R.string.request_file_permissions))
                    .setPositiveButton(activity.getString(R.string.ok)) { _, _ ->
                        val packageName = activity.packageName
                        val intent = Intent()
                        intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                        intent.data = Uri.fromParts("package", packageName, null)
                        startActivity(activity, intent, null)
                    }
                    .setNeutralButton(activity.getString(R.string.ask_me_later)){ _, _ ->

                    }
//                            .setNegativeButton(activity.getString(R.string.deny_dont_ask_Again)) { _, _ ->
//                               ConfigsUtils.setPermissionRemind(activity,false,0)
//                            }
                builder.show()
            }
        }

    }
    fun checkSelfExternalStoragePermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
            val permission = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            permission.forEach {
                areSelfExternalStoragePermissionEnabled = (context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED)
            }
        }
        else{
            areSelfExternalStoragePermissionEnabled = Environment.isExternalStorageManager()
        }
    }
    var areNotificationsEnabled  by mutableStateOf(true)
        private set
    fun requestNotificationsPermission(activity: ComponentActivity){
        val manager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 检测该应用是否有通知权限
        areNotificationsEnabled=manager.areNotificationsEnabled()
        // && Build.VERSION.SDK_INT>= Build.VERSION_CODES.TIRAMISU
        val requestPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                areNotificationsEnabled=isGranted
            }
        if(!areNotificationsEnabled){
            if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    fun checkNotificationsPermission(context: Context){
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 检测该应用是否有通知权限
        areNotificationsEnabled=manager.areNotificationsEnabled()
    }
}





//    fun checkSelfExternalStoragePermission(activity: Activity){
//        val permission = arrayOf(
//            Manifest.permission.WRITE_EXTERNAL_STORAGE,
//            Manifest.permission.READ_EXTERNAL_STORAGE
//        )
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
//            val REQUEST_CODE_CONTACT = 101
//            permission.forEach {
//                if (activity.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED) {
//                    if(!ActivityCompat.shouldShowRequestPermissionRationale(activity,it)){
//                        activity.requestPermissions(permission, REQUEST_CODE_CONTACT)
//                    }
//                    areSelfExternalStoragePermissionEnabled= false
//                } else {
////                    if (it.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
////                        Log.d("PermissionManager","读权限")
////                        Toast.makeText(
////                            activity,
////                            "已获得访问Android文件读权限",
////                            Toast.LENGTH_SHORT
////                        ).show()
////                    }
////                    if (it.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
////                        Toast.makeText(
////                            activity,
////                            "已获得访问Android文件写权限",
////                            Toast.LENGTH_SHORT
////                        ).show()
////                    }
//                    areSelfExternalStoragePermissionEnabled=true
//                }
//            }
//        }
//        else{
//            if(Environment.isExternalStorageManager()){
////                Toast.makeText(
////                    activity,
////                    "已获得访问Android所有文件权限",
////                    Toast.LENGTH_SHORT
////                ).show()
//                areSelfExternalStoragePermissionEnabled=true
//            }
//            else {
//                areSelfExternalStoragePermissionEnabled = false
//                if (!ActivityCompat.shouldShowRequestPermissionRationale(activity,permission[0])||ActivityCompat.shouldShowRequestPermissionRationale(activity, permission[1])){
//                val builder = AlertDialog.Builder(activity)
//                    .setMessage(activity.getString(R.string.request_file_permissions))
//                    .setPositiveButton(activity.getString(R.string.ok)) { _, _ ->
////                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
//                        val intent = Intent()
//                        val packageName = activity.packageName
//                        intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
//                        intent.data = Uri.fromParts("package", packageName, null)
//                        //intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
//                        startActivity(activity, intent, null)
//                    }
//                builder.show()
//            }
//            }
//        }
//    }
//    if(ConfigsUtils.notificationsRemind) {
//        val builder = AlertDialog.Builder(activity)
//            .setMessage(activity.getString(R.string.request_notification_permissions))
//            .setPositiveButton(activity.getString(R.string.ok)) { _, _ ->
////                        val intent = Intent()
////                        val packageName = activity.packageName
////                        intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
////                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
//                val intent = Intent().apply {
//                    when {
//                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
//                            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
//                            putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
//                        }
//                        // For older versions, you might need to guide them to the general app info screen
//                        // from where they can access notification settings.
//                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
//                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
//                            data = Uri.fromParts("package", activity.packageName, null)
//                        }
//                        else -> {
//                            action =
//                                Settings.ACTION_SETTINGS // Fallback to general settings
//                        }
//                    }
//                }
//                startActivity(activity, intent, null)
//            }
//            .setNeutralButton(activity.getString(R.string.ask_me_later)){ _, _ ->
//
//            }
//            .setNegativeButton(activity.getString(R.string.deny_dont_ask_Again)) { _, _ ->
//                ConfigsUtils.setPermissionRemind(activity,false,1)
//            }
//        builder.show()
//    }