package com.example.audioandvideoeditor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.audioandvideoeditor.components.HomeScreen
import com.example.audioandvideoeditor.dao.AppDatabase
import com.example.audioandvideoeditor.dao.TasksDao
import com.example.audioandvideoeditor.services.TasksBinder
import com.example.audioandvideoeditor.services.TasksService
import com.example.audioandvideoeditor.ui.theme.AudioAndVideoEditorTheme
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.utils.FirebaseUtils
import com.example.audioandvideoeditor.utils.LogUtils
import com.example.audioandvideoeditor.utils.PermissionsUtils
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private val TAG="MainActivity"
    lateinit var tasksBinder: TasksBinder
        private set
    lateinit var tasksDao: TasksDao
    var tasks_binder_flag by mutableStateOf(false)
        private set
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            tasksBinder = service as TasksBinder
//            tasksBinder.initTasksDao(tasksDao)
            Log.d(TAG,"tasksBinder = service as TasksBinder")
            tasks_binder_flag=true
        }
        override fun onServiceDisconnected(name: ComponentName) {
        }
    }
    private var showCrashMessageFlag by mutableStateOf(false)
    private var crashMessageText by mutableStateOf("")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupCrashHandler()
        setContent {
            AudioAndVideoEditorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if(tasks_binder_flag){
                        HomeScreen(this)//Greeting(ffmpegInfo())
                    }
                }
//                if(showCrashMessageFlag){
//                    showCrashMessage(message = crashMessageText, context = LocalContext.current) {
//                        handleCrash()
//                    }
//                }
            }
        }
        tasksDao=AppDatabase.getDatabase(this).taskDao()
        val intent = Intent(this, TasksService::class.java)
        startService(intent)
        tasks_binder_flag=false
        bindService(intent, connection, Context.BIND_AUTO_CREATE) // 绑定Service
        PermissionsUtils.requestSelfExternalStoragePermission(this)
        PermissionsUtils.requestNotificationsPermission(this)
        //PermissionsUtils.requestIgnoreBatteryOptimizations(this)
        if(!FirebaseUtils.init_flag){
            FirebaseUtils.initFirebase(this)
        }
        else{
            FirebaseUtils.reFreshRemoteConfig()
        }
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
        PermissionsUtils.checkNotificationsPermission(this)
        PermissionsUtils.checkSelfExternalStoragePermission(this)
        PermissionsUtils.isIgnoringBatteryOptimizations(this)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
        if(tasksBinder.getRemainingTasksNum()==0){
            val intent = Intent(this,TasksService::class.java)
            stopService(intent)
        }
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
        intent = Intent(this,MainActivity::class.java)
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
        val toast = Toast.makeText( this, getString(R.string.feedback_text3), Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
        stopService(intent)
        intent = Intent(this,MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        this.finish()
    }

    private external fun ffmpegInfo():String
    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }
}


@Composable
fun showCrashMessage(message: String,context: Context,restart:()->Unit) {
    AlertDialog(
        onDismissRequest = {

        },
        title = {
          Text("出错了")
        },
        text = {
           Column (
               modifier = Modifier.fillMaxWidth()
           ){
               Text("出错了，请复制报错信息发送给开发者")
               LazyColumn(
                   modifier = Modifier
                       .fillMaxWidth()
                       .background(
                           color = MaterialTheme.colorScheme.background,
                           shape = RoundedCornerShape(10.dp)
                       )
               ){
                   item {
                       Row(
                           modifier = Modifier.fillMaxWidth(),
                           horizontalArrangement = Arrangement.End
                       ) {
                           IconButton(onClick = {
                               val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                               val clip = ClipData.newPlainText("logs", message)
                               clipboard.setPrimaryClip(clip)
                               val toast = Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT)
                               toast.setGravity(Gravity.CENTER, 0, 0)
                               toast.show()
                           }) {
                               Icon(
                                   painter = painterResource(id = R.drawable.baseline_content_copy_24)
                                   , contentDescription = null)
                           }
                       }
                   }
                   item {
                        Text(message)
                    }
               }
           }
        },
        confirmButton = {
            Button(onClick = {
                restart()
            }) {
                Text("重启")
            }
        },
        dismissButton = {
        }
    )
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