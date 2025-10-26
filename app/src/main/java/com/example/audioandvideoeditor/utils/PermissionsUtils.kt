package com.example.audioandvideoeditor.utils

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.findActivity


private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val packageName = context.packageName
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(packageName)
}
@Composable
fun observePermissionStatus(permission: String): State<Boolean> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ① 使用 remember 存储权限的当前状态（默认为初始检查结果）
    val isGranted = remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
    }

    // ② 使用 LaunchedEffect 监听 ON_RESUME 事件
    // 当生命周期处于 RESUMED 状态时，重新检查权限
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val isCurrentGranted = ContextCompat.checkSelfPermission(
                context, permission
            ) == PackageManager.PERMISSION_GRANTED

            // 只有状态发生变化时才更新 State，触发重组
            if (isGranted.value != isCurrentGranted) {
                isGranted.value = isCurrentGranted
            }
        }
    }

    return isGranted
}
@Composable
fun observeIgnoringBatteryPermissionStatus(): State<Boolean> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // ① 使用 remember 存储权限的当前状态（默认为初始检查结果）
    val isGranted = remember {
        mutableStateOf(isIgnoringBatteryOptimizations(context))
    }
    // ② 使用 LaunchedEffect 监听 ON_RESUME 事件
    // 当生命周期处于 RESUMED 状态时，重新检查权限
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val isCurrentGranted =isIgnoringBatteryOptimizations(context)
            // 只有状态发生变化时才更新 State，触发重组
            if (isGranted.value != isCurrentGranted) {
                isGranted.value = isCurrentGranted
            }
        }
    }
    return isGranted
}

fun checkNotificationsPermission(context: Context): Boolean {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    // 检测该应用是否有通知权限
    return manager.areNotificationsEnabled()
}

@Composable
fun observeNotificationsPermissionStatus(): State<Boolean> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // ① 使用 remember 存储权限的当前状态（默认为初始检查结果）
    val isGranted = remember {
        mutableStateOf(checkNotificationsPermission(context))
    }
    // ② 使用 LaunchedEffect 监听 ON_RESUME 事件
    // 当生命周期处于 RESUMED 状态时，重新检查权限
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val isCurrentGranted =checkNotificationsPermission(context)
            // 只有状态发生变化时才更新 State，触发重组
            if (isGranted.value != isCurrentGranted) {
                isGranted.value = isCurrentGranted
            }
        }
    }
    return isGranted
}

@Composable
fun observeStoragePermissionStatus(): State<Boolean> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // ① 使用 remember 存储权限的当前状态（默认为初始检查结果）
    val isGranted = remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
                ( ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED )
                && (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED )
            }
            else{
                Environment.isExternalStorageManager()
            }
        )
    }
    // ② 使用 LaunchedEffect 监听 ON_RESUME 事件
    // 当生命周期处于 RESUMED 状态时，重新检查权限
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val isCurrentGranted =(
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
                        ( ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED )
                                && (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED )
                    }
                    else{
                        Environment.isExternalStorageManager()
                    }
                    )
            // 只有状态发生变化时才更新 State，触发重组
            if (isGranted.value != isCurrentGranted) {
                isGranted.value = isCurrentGranted
            }
        }
    }
    return isGranted
}


@Composable
fun PermissionRequestTemplate(
    permission: String,
    text:String= stringResource(id = R.string.request_permission),
   // content: @Composable () -> Unit, // 权限授予后显示的内容
    rationaleContent: @Composable (
        // 解释理由后，重新发起请求的Action
        onRequestPermission: () -> Unit,
        // 永久拒绝后，跳转到设置页的Action
        onOpenSettings: () -> Unit
    ) -> Unit,
   // 永久拒绝后，跳转到设置页的Action
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ① 核心状态：跟踪权限是否已授予
    var isPermissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
    }
    // 核心状态：跟踪是否需要展示“理由解释”UI
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showPermissionPromptDialog by remember { mutableStateOf(true) }
    // ② 注册权限请求启动器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            isPermissionGranted = isGranted // 更新状态
            // 如果被拒绝，检查是否需要弹出 Rationale（尽管系统对话框已关闭，但状态仍需更新）
            if (!isGranted) {
                val activity = context.findActivity()
                showRationaleDialog = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            }
        }
    )

    // ③ 权限状态检查与 UI 渲染
    when {
        // 状态 1: 权限已授予 -> 显示核心功能
        isPermissionGranted -> {
            //content()
        }

        // 状态 2: 需要展示 Rationale -> 显示理由解释 UI
        showRationaleDialog -> {
            RationaleDisplay(
                rationaleContent = rationaleContent,
                onDismiss = { showRationaleDialog = false },
                onRequest = {
                    // 重新发起请求
                    permissionLauncher.launch(permission)
                    showRationaleDialog = false // 请求发起后隐藏对话框
                },
                onOpenSettings=onOpenSettings
            )
        }

        // 状态 3: 初始或拒绝状态 -> 显示请求按钮/提示
        else -> {
            // 首次请求，或用户已永久拒绝（shouldShowRationale = false）
            if(showPermissionPromptDialog){
                AlertDialog(
                    onDismissRequest = {showPermissionPromptDialog=false},
                    title = {
                        Text(text)
                    },
                    text = {

                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val activity = context.findActivity()
                            // 再次检查是否需要显示理由
                            if (ActivityCompat.shouldShowRequestPermissionRationale(
                                    activity,
                                    permission
                                )
                            ) {
                                showRationaleDialog = true
                            } else {
                                // 首次请求，或用户已永久拒绝，直接启动请求
                                permissionLauncher.launch(permission)
                            }
                        }) {
                            Text(stringResource(id = R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showPermissionPromptDialog=false
                        }) {
                            Text(stringResource(id = R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

// A. 权限提示按钮 (State 3 UI)
@Composable
fun PermissionPromptButton(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("需要权限才能使用此功能。")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onClick) {
            Text("立即授权")
        }
    }
}

@Composable
fun PermissionPrompt(showPermissionPromptDialo:Boolean) {

}

// B. 理由展示对话框 (State 2 UI)
@Composable
fun RationaleDisplay(
    rationaleContent: @Composable (onRequestPermission: () -> Unit, onOpenSettings: () -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {

        },
        text = {
            rationaleContent(
                onRequestPermission = onRequest, // 重新请求的 Action
                onOpenSettings = onOpenSettings // 跳转设置的 Action
            )
        },
        confirmButton = {
        },
        dismissButton = {
              TextButton(onClick = {onDismiss()}) {
                  Text(text = stringResource(id = R.string.cancel))
              }
        }
    )
}

// C. 跳转设置工具函数 (用于永久拒绝处理)
fun Context.openAppSettings() {
    // Intent 跳转到应用的设置详情页
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(this)
    }
}


/*
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

    /**
     * 检查应用是否被忽略电池优化。
     * @return 如果应用被忽略，则返回 true。
     */
    var ignoringBatteryOptimizationsEnabled  by mutableStateOf(true)
        private set
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val packageName = context.packageName
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        ignoringBatteryOptimizationsEnabled=pm.isIgnoringBatteryOptimizations(packageName)
        return ignoringBatteryOptimizationsEnabled
    }
    /**
     * 引导用户到电池优化设置页面。
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if(!isIgnoringBatteryOptimizations(context)){
            val builder = AlertDialog.Builder(context)
                .setMessage(context.getString(R.string.request_battery_permissions))
                .setPositiveButton(context.getString(R.string.ok)) { _, _ ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent().apply {
                            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                            data = Uri.parse("package:" + context.packageName)
                        }
                        context.startActivity(intent)
                    }
                }
                .setNeutralButton(context.getString(R.string.ask_me_later)){ _, _ ->

                }
            builder.show()
        }
    }

    var recordAudioEnabled by mutableStateOf(false)
        private set
    fun requestRecordAudioPermission(activity: ComponentActivity){
        val requestRecordAudioLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
              recordAudioEnabled=isGranted
        }
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestRecordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    fun checkRecordAudioPermission(context: Context){
        recordAudioEnabled=(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
}

*/



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