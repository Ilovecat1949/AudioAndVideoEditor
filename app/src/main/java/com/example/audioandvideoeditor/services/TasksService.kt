package com.example.audioandvideoeditor.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.audioandvideoeditor.IFFmpegService
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.dao.AppDatabase
import com.example.audioandvideoeditor.dao.TasksDao
import com.example.audioandvideoeditor.entity.Task
import com.example.audioandvideoeditor.utils.PermissionsUtils
import com.example.audioandvideoeditor.entity.TaskInfo
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.utils.FilesUtils
import java.lang.Thread.sleep
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

class TasksService : Service() {
    private val TAG="TasksService"
    private var m_tasksFactory: Long = -1
    private var ID_NUM = 0L
    private var MAX_TASKS_NUM=2
    private val mBinder = TasksBinder(this)
    private lateinit var tasksDao: TasksDao
    private var iFFmpegServiceFlag=false
    private var iFFmpegService:IFFmpegService?=null
    val mConnection = object : ServiceConnection {
        // Called when the connection with the service is established.
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // Following the preceding example for an AIDL interface,
            // this gets an instance of the IFFmpegInterface, which we can use to call on the service.
            iFFmpegService = IFFmpegService.Stub.asInterface(service)
            serviceConnectedLatch?.countDown()
        }
        // Called when the connection with the service disconnects unexpectedly.
        override fun onServiceDisconnected(className: ComponentName) {
            Log.e(TAG, "iFFmpegService has unexpectedly disconnected")
            iFFmpegService = null
        }
    }
    override fun onCreate() {
        super.onCreate()
        m_tasksFactory=initTasksFactory()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "TasksBinder", "TasksBinder",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        tasksDao= AppDatabase.getDatabase(this).taskDao()
        //ID_NUM=tasksDao.getTaskNum()
        thread {
            val max_id=tasksDao.getMaxTaskId()
            ID_NUM = if (max_id != null) {
                max_id+1
            }else{
                0
            }
        }
        MAX_TASKS_NUM=ConfigsUtils.MAX_TASKS_NUM
        Log.d(TAG,"TasksService Create ")
    }
    override fun onBind(intent: Intent): IBinder {
        //TODO("Return the communication channel to the service.")
        return mBinder
    }
    private var monitor_flag=false
    private val waitingTasksQueue = LinkedList<TaskInfo>()
    private val runningTasksQueue = LinkedList<TaskInfo>()
    private val waitingCancelTaskIdQueue = LinkedList<Long>()
    private val TasksQueue=LinkedList<TaskInfo>()
    private val taskStateMap=HashMap<Long,Int>()
    private val notificationMap=HashMap<Long, NotificationCompat.Builder>()
    private val progressMap=HashMap<Long,Float>()
    private lateinit var notificationManager: NotificationManager
    private val lock = ReentrantLock()
    private var serviceConnectedLatch:CountDownLatch?=null
    private fun MonitorTask(){
        thread {
            notificationMap.clear()
            while(monitor_flag) {
                var i = 0
                lock.lock()
                while(i<waitingCancelTaskIdQueue.size){
                    val id=waitingCancelTaskIdQueue[i]
                    cancelTask2(id)
                    i++
                }
                waitingCancelTaskIdQueue.clear()
                lock.unlock()
                i=0
                while (i < runningTasksQueue.size) {
                    var state:Int=taskStateMap[runningTasksQueue[i].long_arr[0]]!!
                    if(state==0) {
                        state = if(runningTasksQueue[i].int_arr[0]<2){
                            getTaskState(m_tasksFactory, runningTasksQueue[i].long_arr[0])
                        } else{
                            iFFmpegService!!.getTaskState(runningTasksQueue[i].long_arr[0])
                        }
                        taskStateMap[runningTasksQueue[i].long_arr[0]]=state
                    }
//                    Log.d(TAG,"state:"+state)
                    if(state==0)
                    {
                        var progress: Float
                        progress = if(runningTasksQueue[i].int_arr[0]<2) {
                            getProgress(m_tasksFactory, runningTasksQueue[i].long_arr[0])
                        } else{
                           iFFmpegService!!.getProgress(runningTasksQueue[i].long_arr[0])
                        }
                        progressMap[runningTasksQueue[i].long_arr[0]]=progress
                        if(PermissionsUtils.areNotificationsEnabled && notificationMap.containsKey(runningTasksQueue[i].long_arr[0])) {
                            val notification = notificationMap[runningTasksQueue[i].long_arr[0]]
                            val position = (if(progress<0) 0 else if(progress>1) 99.99 else progress*100)
//                            Log.d(TAG, "position:" + position + "progress:" + progress)
                            if(runningTasksQueue[i].int_arr[0]!=2) {
                                notification!!.setProgress(100, position.toInt(), false)
                                notification.setContentText("${position.toInt()}%")
                                notificationManager.notify(
                                    runningTasksQueue[i].long_arr[0].toInt(),
                                    notification.build()
                                )
                            }
                        }
                        i++
                    }
                    else{
                        if(PermissionsUtils.areNotificationsEnabled && notificationMap.containsKey(runningTasksQueue[i].long_arr[0])) {
                            if(state==1) {
                                val notification = notificationMap[runningTasksQueue[i].long_arr[0]]
                                notification!!.setProgress(100, 100, false)
                                notification.setContentText(getString(R.string.end_of_execution))
//                                notification.setContentTitle("已完成任务")
                                notificationManager.notify(
                                    runningTasksQueue[i].long_arr[0].toInt(),
                                    notification.build()
                                )
                            }
                            else if(state==2){
                                val notification = notificationMap[runningTasksQueue[i].long_arr[0]]
                                notification!!.setProgress(100, 0, false)
                                notification.setContentText(getString(R.string.cancel_execution))
//                                notification.setContentTitle("已取消任务")
                                notificationManager.notify(
                                    runningTasksQueue[i].long_arr[0].toInt(),
                                    notification.build()
                                )
                            }
                            else if(state==-1){
                                val notification = notificationMap[runningTasksQueue[i].long_arr[0]]
                                notification!!.setProgress(100, 0, false)
                                notification.setContentText(getString(R.string.execution_failed))
//                                notification.setContentTitle("任务失败")
                                notificationManager.notify(
                                    runningTasksQueue[i].long_arr[0].toInt(),
                                    notification.build()
                                )
                            }
                        }
//                        if(notificationMap.containsKey(runningTasksQueue[i].long_arr[0])) {
//                            notificationMap.remove(runningTasksQueue[i].long_arr[0])
//                        }
                        progressMap.remove(runningTasksQueue[i].long_arr[0])
                        if(runningTasksQueue[i].int_arr[0]<2){
                            releaseTask(m_tasksFactory, runningTasksQueue[i].long_arr[0])
                        }
                        else {
                            iFFmpegService!!.releaseTask(runningTasksQueue[i].long_arr[0])
                        }
                        val date= Date(System.currentTimeMillis())
                        val formatter= SimpleDateFormat("yyyy-MM-dd HH:mm:ss", resources.configuration.locales[0])
                        tasksDao.insertTask(
                            Task(
                                runningTasksQueue[i].long_arr[0],
                                runningTasksQueue[i].int_arr[0],
                                state,
                                runningTasksQueue[i].str_arr[0],
                                runningTasksQueue[i].str_arr[1],
                                formatter.format(date)
                            )
                        )
                        runningTasksQueue.removeAt(i)
                    }
                }
                val free_num = MAX_TASKS_NUM - runningTasksQueue.size
                i = 0
                while (i < free_num) {
                    if (waitingTasksQueue.size < 1) {
                        break
                    } else {
                        val info = waitingTasksQueue[0]

                        //createAndStartTask(info)
                        var state=0
                        if(info.int_arr[0]<2) {
                            state = createAndStartTask(
                                m_tasksFactory,
                                info.int_arr.toIntArray(),
                                info.long_arr.toLongArray(),
                                info.float_arr.toFloatArray(),
                                info.str_arr.toTypedArray()
                            )
                            waitingTasksQueue.removeAt(0)
                            runningTasksQueue.add(info)
                        }
                        else{
                           if(!iFFmpegServiceFlag){
                               val intent = Intent(this, FFmpegService::class.java)
                               bindService(intent, mConnection, Context.BIND_AUTO_CREATE) // 绑定Service
                               iFFmpegServiceFlag=true
                               serviceConnectedLatch=CountDownLatch(1)
                               sleep(100)
                           }
                            if(iFFmpegService==null){
                                Log.d(TAG,"iFFmpegService==null")
                                break
                            }
                            state =iFFmpegService!!.createAndStartTask(
                                info.int_arr.toIntArray(),
                                info.long_arr.toLongArray(),
                                info.str_arr.toTypedArray(),
                                info.float_arr.toFloatArray()
                            )
                            waitingTasksQueue.removeAt(0)
                            runningTasksQueue.add(info)
                        }
                        taskStateMap[info.long_arr[0]]=state
                        progressMap[info.long_arr[0]]=0f
                        if(PermissionsUtils.areNotificationsEnabled) {
                            val name=FilesUtils.getNameFromPath(info.str_arr[0])
                            val notification =
                                if(info.int_arr[0]!=2){
                                 NotificationCompat.Builder(this@TasksService, "TasksBinder")
                                    .setContentTitle(name)
                                    .setContentText("0%")
                                    .setProgress(100, 0, false)
                                    .setPriority(NotificationManager.IMPORTANCE_LOW)
                                    .setSmallIcon(R.drawable.movie_edit_24px)
                                    .setAutoCancel(false)
                                    .setSilent(true)
                                }
                            else{
                                 NotificationCompat.Builder(this@TasksService, "TasksBinder")
                                        .setContentTitle(name.substring(0,name.length-4))
                                        .setPriority(NotificationManager.IMPORTANCE_LOW)
                                        .setSmallIcon(R.drawable.movie_edit_24px)
                                        .setAutoCancel(false)
                                        .setSilent(true)
                                }
                            notificationManager.notify(
                                info.long_arr[0].toInt(),
                                notification.build()
                            )
                            notificationMap[info.long_arr[0]] = notification
                        }
                    }
                    i++
                }
                if(waitingTasksQueue.size==0 && runningTasksQueue.size==0){
                    break
                }
                serviceConnectedLatch?.await()
            }
            monitor_flag=false
        }
    }
    fun startTask(info: TaskInfo){
        info.long_arr.add(0,ID_NUM)
        ID_NUM++
        waitingTasksQueue.add(info)
        TasksQueue.add(info)
        if(!monitor_flag){
            monitor_flag=true
            MonitorTask()
        }
    }
//    fun initTasksDao(dao: TasksDao){
//        tasksDao=dao
//        val intent=Intent(this,TasksService::class.java)
//    }
    private fun createAndStartTask(info: TaskInfo){
        val _int_arr=ArrayList<Int>()
        _int_arr.add(-1)
        if(info.int_arr.size>0){
            _int_arr.addAll(info.int_arr)
        }
        val _long_arr=ArrayList<Long>()
        _long_arr.add(-1L)
        if(info.long_arr.size>0){
            _long_arr.addAll(info.long_arr)
        }
        val _float_arr=ArrayList<Float>()
        _float_arr.add(-1f)
        if(info.float_arr.size>0){
            _float_arr.addAll(info.float_arr)
        }
        val _str_arr=ArrayList<String>()
        _str_arr.add("\\0")
        if(info.str_arr.size>0){
            _str_arr.addAll(info.str_arr)
        }
        createAndStartTask(
                    m_tasksFactory,
                    _int_arr.toIntArray(),
                    _long_arr.toLongArray(),
                    _float_arr.toFloatArray(),
                    _str_arr.toTypedArray()
                )
    }
    fun cancelTask(id: Long){
        lock.lock()
        waitingCancelTaskIdQueue.add(id)
        lock.unlock()
    }
    private fun cancelTask2(id: Long){
        var i=0
        while(i<waitingTasksQueue.size){
            if(waitingTasksQueue[i].long_arr[0]==id){
                waitingTasksQueue.removeAt(i)
                taskStateMap[id]=2
                break
            }
            i++
        }
        i=0
        while(i<runningTasksQueue.size){
            if(runningTasksQueue[i].long_arr[0]==id && runningTasksQueue[i].int_arr[0]>=2){
//                runningTasksQueue.removeAt(i)
//                taskStateMap[id]=2
                iFFmpegService!!.cancelTask(id)
//                unbindService(mConnection)
//                iFFmpegServiceFlag=false
//                iFFmpegService = null
//                sleep(100)
                break
            }
            i++
        }
        if(!taskStateMap.containsKey(id)
            ||(taskStateMap[id]==0)
        ){
            cancelTask(m_tasksFactory,id)
        }
    }
//    fun cancelTask(id: Long){
//        var i=0
//        while(i<watingTasksQueue.size){
//            if(watingTasksQueue[i].long_arr[0]==id){
//                watingTasksQueue.removeAt(i)
//                taskStateMap[id]=2
//                break
//            }
//            i++
//        }
//        i=0
//        while(i<runningTasksQueue.size){
//            if(runningTasksQueue[i].long_arr[0]==id && runningTasksQueue[i].int_arr[0]>=2){
//                runningTasksQueue.removeAt(i)
//                taskStateMap[id]=2
//                if(iFFmpegService==null){
//                    thread{
//                        while(iFFmpegService==null){
//                            sleep(100)
//                        }
//                        unbindService(mConnection)
//                        iFFmpegService=null
//                    }
//                }
//                else{
//                    unbindService(mConnection)
//                    iFFmpegService=null
//                }
//                break
//            }
//            i++
//        }
//        if(!taskStateMap.containsKey(id)
//            ||(taskStateMap[id]==0)
//            ){
//            cancelTask(m_tasksFactory,id)
//        }
//    }
    fun getAVStrInfo(path:String):String{
        return getAudioAndVideoStrInfo(path)
    }
    fun getRemainingTasksNum():Int{
       return waitingTasksQueue.size+runningTasksQueue.size
    }
    fun getTaskState(id:Long):Int{
        return if(taskStateMap.containsKey(id)){
            taskStateMap[id]!!
        } else{
            -1
        }

    }
    fun getTasksQueue():List<TaskInfo>{
        return TasksQueue
    }
    fun getWatingTasksQueue():List<TaskInfo>{
        return waitingTasksQueue
    }
    fun getRunningTasksQueue():List<TaskInfo>{
        return runningTasksQueue
    }
    fun getTaskProgress(id:Long):Float{
        return if(progressMap.containsKey(id)) {
            progressMap[id]!!
        } else{
            0f
        }
    }
    fun getFFmpegInfo(info_type:Int):String{
        return getFFmpegStrInfo(info_type)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(ConfigsUtils.setCurrLanguageMode(newBase))
    }
    override fun onDestroy() {
        super.onDestroy()
        try {
            monitor_flag=false
            var i=0
            while(i<runningTasksQueue.size){
                cancelTask(m_tasksFactory, runningTasksQueue[i].long_arr[0])
                releaseTask(m_tasksFactory, runningTasksQueue[i].long_arr[0])
                i++
            }
            i=0
            while(i<waitingTasksQueue.size){
                releaseTask(m_tasksFactory, waitingTasksQueue[i].long_arr[0])
                i++
            }
        }
        catch (e:Exception){
            Log.d(TAG,e.stackTraceToString())
        }
        if(iFFmpegService!=null) {
            unbindService(mConnection)
        }
    }
    private external fun initTasksFactory():Long
    private external fun createAndStartTask(m_tasksFactory: Long,
                                            int_arr:IntArray,
                                            long_arr:LongArray,
                                            float_arr:FloatArray,
                                            str_arr:Array<String>):Int
    private external fun getTaskState(m_tasksFactory: Long,taskID:Long):Int
    private external fun releaseTask(m_tasksFactory: Long,taskID:Long)
    private external fun cancelTask(m_tasksFactory: Long,taskID:Long)
    private external fun getProgress(m_tasksFactory: Long,taskID:Long):Float
    private external fun getAudioAndVideoStrInfo(path:String):String
    private external fun getFFmpegStrInfo(info_type:Int):String
    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }
}