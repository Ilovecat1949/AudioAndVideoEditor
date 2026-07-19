package com.example.audioandvideoeditor

//import com.example.audioandvideoeditor.utils.PermissionsUtils
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.audioandvideoeditor.application.AppApplication.Companion.INSTANCE
import com.example.audioandvideoeditor.components.HomeScreen
import com.example.audioandvideoeditor.services.TaskService
import com.example.audioandvideoeditor.services.TasksBinder
import com.example.audioandvideoeditor.ui.theme.AudioAndVideoEditorTheme
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.utils.LogUtils
import com.example.audioandvideoeditor.utils.PermissionRequestTemplate
import kotlinx.coroutines.launch

fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    // 权限请求必须在 Activity Context 中进行
    throw IllegalStateException("Permissions must be requested within an Activity Context.")
}
class MainActivity : ComponentActivity() {
    private val TAG="MainActivity"

    // 🌟 核心保底锁：缓存来自通知栏待跳转的路由目标
    // 使用 String 类型，和你的 Compose 路由（Destination）完全对齐
    private var pendingNotificationRoute: String? = null
    /**
     * 核心稳妥解析器：只负责安全的将 Intent 里的参数提取出来
     */
    private fun handleNotificationIntent(intent: Intent?) {
        intent?.getStringExtra("TARGET_ROUTE")?.let { route ->
            // 抓取到了通知栏传来的目标路由卡片
            pendingNotificationRoute = route
            // 💡 稳妥微调：立刻在底层的原始 Intent 中擦除它，防止配置变更（如旋转屏幕）导致 onCreate 重新读取旧数据
            intent.removeExtra("TARGET_ROUTE")
        }
    }

    // 直接获取全局仓库
// Activity 中安全获取
    private val repository by lazy {
        INSTANCE.taskRepository
    }

    private lateinit var serviceConnection: ServiceConnection

    private var showCrashMessageFlag by mutableStateOf(false)
    private var crashMessageText by mutableStateOf("")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupCrashHandler()

        // 🌟 2. 拦截冷启动（App进程已死，点击通知唤醒）
        handleNotificationIntent(intent)

        setContent {
            AudioAndVideoEditorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
//                    if(tasks_binder_flag){
                        HomeScreen(
                            initialRoute = pendingNotificationRoute,
                            onRouteConsumed = {
                                // 消费回调：一旦跳转成功，立刻切断指针，防止旋转屏幕重复跳转
                                pendingNotificationRoute = null
                            }
                        )//Greeting(ffmpegInfo())
//                    }
//                    GreetingPreview()
                    val context= LocalContext.current
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
                        PermissionRequestTemplate(
                            permission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            text= stringResource(id = R.string.request_write_permission),
                            rationaleContent = { onRequest, onOpenSettings ->
                                Column {
                                    Text(text = stringResource(id = R.string.read_and_write_permissions), fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row {
                                        // 引导用户再次请求
                                        Button(onClick = onRequest) { Text(stringResource(id = R.string.reauthorization)) }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // 引导用户进入设置页（处理永久拒绝场景）
                                        Button(onClick = onOpenSettings) { Text(stringResource(id = R.string.go_to_settings)) }
                                    }
                                }

                            },
                            onOpenSettings = {
                                val packageName = context.packageName
                                val intent = Intent()
                                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                intent.data = Uri.fromParts("package", packageName, null)
                                ContextCompat.startActivity(context, intent, null)
                            }
                        )
                        PermissionRequestTemplate(
                            permission = Manifest.permission.READ_EXTERNAL_STORAGE,
                            text=stringResource(id = R.string.request_read_permission),
                            rationaleContent = { onRequest, onOpenSettings ->
                                Column {
                                    Text(
                                        text = stringResource(id = R.string.read_and_write_permissions),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row {
                                        // 引导用户再次请求
                                        Button(onClick = onRequest) { Text(stringResource(id = R.string.reauthorization)) }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // 引导用户进入设置页（处理永久拒绝场景）
                                        Button(onClick = onOpenSettings) { Text(stringResource(id = R.string.go_to_settings)) }
                                    }
                                }
                            },
                            onOpenSettings = {
                                val packageName = context.packageName
                                val intent = Intent()
                                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                intent.data = Uri.fromParts("package", packageName, null)
                                ContextCompat.startActivity(context, intent, null)
                            }
                        )
                    }
                    else{
                        val areSelfExternalStoragePermissionEnabled = Environment.isExternalStorageManager()
                        if(!areSelfExternalStoragePermissionEnabled){
                            val activity = context.findActivity()
                            val builder = AlertDialog.Builder(activity)
                                .setMessage(activity.getString(R.string.request_file_permissions))
                                .setPositiveButton(activity.getString(R.string.ok)) { _, _ ->
                                    val packageName = activity.packageName
                                    val intent = Intent()
                                    intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                                    intent.data = Uri.fromParts("package", packageName, null)
                                    ContextCompat.startActivity(activity, intent, null)
                                }
                                .setNeutralButton(activity.getString(R.string.ask_me_later)){ _, _ ->

                                }
                            builder.show()
                        }
                    }
                    if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.TIRAMISU) {
                        PermissionRequestTemplate(
                            permission = Manifest.permission.POST_NOTIFICATIONS,
                            text= stringResource(id = R.string.request_notification_permission),
                            rationaleContent = { onRequest, onOpenSettings ->
                                Column {
                                    Text(
                                        text = stringResource(id = R.string.apply_for_notification_permission),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row {
                                        // 引导用户再次请求
                                        Button(onClick = onRequest) { Text(stringResource(id = R.string.reauthorization)) }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // 引导用户进入设置页（处理永久拒绝场景）
                                        Button(onClick = onOpenSettings) { Text(stringResource(id = R.string.go_to_settings)) }
                                    }
                                }
                            },
                            onOpenSettings = {
                                val intent = Intent().apply {
                                    when {
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                                            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                            putExtra(
                                                Settings.EXTRA_APP_PACKAGE,
                                                context.packageName
                                            )
                                        }
                                        // For older versions, you might need to guide them to the general app info screen
                                        // from where they can access notification settings.
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                            data =
                                                Uri.fromParts("package", context.packageName, null)
                                        }

                                        else -> {
                                            action =
                                                Settings.ACTION_SETTINGS // Fallback to general settings
                                        }
                                    }
                                }
                                ContextCompat.startActivity(context, intent, null)
                            }
                        )
                    }
                }

            }
        }

        // 1. 初始化连接
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                val binder = service as TasksBinder
                repository.setBinder(binder) // 赋值给全局仓库
            }
            override fun onServiceDisconnected(arg0: ComponentName) {
                repository.setBinder(null)
            }
        }

        // 2. 绑定服务（双模式：启动+绑定，保证后台任务不被杀）
        val intent = Intent(this, TaskService::class.java)
        startService(intent) // 关键：启动服务，独立于Activity生命周期
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        lifecycleScope.launch{
            ConfigsUtils.gitHubRelease=ConfigsUtils.getLatestGitHubRelease(getString(R.string.owner),getString(R.string.repo))
            ConfigsUtils.gitHubRelease?.let {
                println("ConfigsUtils.gitHubRelease"+ConfigsUtils.gitHubRelease!!.tagName)
            }

        }
    }
    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//        PermissionsUtils.checkNotificationsPermission(this)
//        PermissionsUtils.checkSelfExternalStoragePermission(this)
//        PermissionsUtils.isIgnoringBatteryOptimizations(this)

    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    override fun onDestroy() {
        super.onDestroy()
//        unbindService(connection)
        unbindService(serviceConnection) // 仅解绑，不停止服务
//        if(tasksBinder.getRemainingTasksNum()==0){
//            val intent = Intent(this,TaskService::class.java)
//            stopService(intent)
//        }
    }
//    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)} passing\n      in a {@link RequestMultiplePermissions} object for the {@link ActivityResultContract} and\n      handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}.")
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//    }
    override fun attachBaseContext(newBase: Context?) {
        if(newBase!=null){
            ConfigsUtils.InitConfig(newBase)
        }
        super.attachBaseContext(ConfigsUtils.setCurrLanguageMode(newBase))
    }
    fun sendToast(text:String){
        val toast = Toast.makeText( this, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show()
    }
    fun setCurrLanguageMode(){
//        unbindService(connection)
//        var intent = Intent(this,TasksService::class.java)
        stopService(intent)
        intent = Intent(this,Context::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        this.finish()
    }

    private fun setupCrashHandler() {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            LogUtils.clearLogs(this)
            LogUtils.log(this, "Crash", "Uncaught Exception: ${throwable.stackTraceToString()}")
            val editor = this.getSharedPreferences("data", Context.MODE_PRIVATE).edit()
            editor.putBoolean("show_crash_message_flag",true)
            editor.apply()
            Log.d(TAG,throwable.stackTraceToString())
            handleCrash()
//            crashMessageText=throwable.stackTraceToString()
//            showCrashMessageFlag=true
        }
    }

    private fun handleCrash() {
//        Toast.makeText(this, "An unexpected error occurred. Please send the logs.", Toast.LENGTH_LONG).show()
        //sendLogsToDeveloper()
//        val toast = Toast.makeText( this, getString(R.string.feedback_text3), Toast.LENGTH_SHORT)
//        toast.setGravity(Gravity.CENTER, 0, 0)
//        toast.show()
        stopService(intent)
        intent = Intent(this, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        this.finish()
    }
private fun showToastFromWorkerThread(context: Context,text: String) {
    // 🌟 获取主线程的 Looper，确保这个 Runnable 一定在主线程队列中排队执行
    Handler(Looper.getMainLooper()).post {
        val toast = Toast.makeText(context, text, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }
}

    private external fun ffmpegInfo():String
    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }
}



@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AudioAndVideoEditorTheme {
        Greeting("Android")
    }
}