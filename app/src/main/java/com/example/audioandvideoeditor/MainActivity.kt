package com.example.audioandvideoeditor

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.audioandvideoeditor.components.HomeScreen
import com.example.audioandvideoeditor.dao.AppDatabase
import com.example.audioandvideoeditor.dao.TasksDao
import com.example.audioandvideoeditor.services.TasksBinder
import com.example.audioandvideoeditor.services.TasksService
import com.example.audioandvideoeditor.ui.theme.AudioAndVideoEditorTheme
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.utils.PermissionsUtils


class MainActivity : ComponentActivity() {
    private val TAG="MainActivity"
    lateinit var tasksBinder: TasksBinder
        private set
    lateinit var tasksDao: TasksDao
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            tasksBinder = service as TasksBinder
//            tasksBinder.initTasksDao(tasksDao)
            Log.d(TAG,"tasksBinder = service as TasksBinder")
        }
        override fun onServiceDisconnected(name: ComponentName) {
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudioAndVideoEditorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(this)//Greeting(ffmpegInfo())
                }
            }
        }
        tasksDao=AppDatabase.getDatabase(this).taskDao()
        val intent = Intent(this, TasksService::class.java)
        startService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE) // 绑定Service
    }
    override fun onResume() {
        super.onResume()
        PermissionsUtils.checkSelfExternalStoragePermission(this)
        PermissionsUtils.checkNotificationsEnabled(this)
    }
    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
        if(tasksBinder.getRemainingTasksNum()==0){
            val intent = Intent(this,TasksService::class.java)
            stopService(intent)
        }
    }
    override fun attachBaseContext(newBase: Context?) {
        if(newBase!=null){
            ConfigsUtils.InitConfig(newBase)
        }
        super.attachBaseContext(ConfigsUtils.setCurrLanguageMode(newBase))
    }
    fun sendToast(text:String){
        val toast = Toast.makeText( this, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
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