package com.example.audioandvideoeditor.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.audioandvideoeditor.IFFmpegService
import com.example.audioandvideoeditor.entity.TaskInfo

class FFmpegService : Service() {
    private var m_tasksFactory: Long = -1
    private val TAG="FFmpegService"
    private var info:TaskInfo? = null
    override fun onCreate() {
        super.onCreate()
        m_tasksFactory=initTasksFactory()
    }
    private val binder = object : IFFmpegService.Stub(){
        override fun basicTypes(
            anInt: Int,
            aLong: Long,
            aBoolean: Boolean,
            aFloat: Float,
            aDouble: Double,
            aString: String?
        ) {
            TODO("Not yet implemented")
        }
        override fun getPid(): Int {
            TODO("Not yet implemented")
        }

        override fun createAndStartTask(
            int_arr: IntArray?,
            long_arr: LongArray?,
            str_arr: Array<out String>?,
            float_arr: FloatArray?
        ):Int {
           val int_arr2=ArrayList<Int>()
           if(int_arr!=null){
               int_arr2.addAll(int_arr.toList())
           }
           val long_arr2=ArrayList<Long>()
           if(long_arr!=null){
               long_arr2.addAll(long_arr.toList())
           }
           val str_arr2=ArrayList<String>()
            if(str_arr!=null){
                str_arr2.addAll(str_arr.toList())
            }
           val float_arr2=ArrayList<Float>()
            if(float_arr!=null){
                float_arr2.addAll(float_arr.toList())
            }
            info= TaskInfo(
                int_arr2,
                long_arr2,
                str_arr2,
                float_arr2
            )
            val state=createAndStartTask(
                m_tasksFactory,
                info!!.int_arr.toIntArray(),
                info!!.long_arr.toLongArray(),
                info!!.float_arr.toFloatArray(),
                info!!.str_arr.toTypedArray()
            )
            return state
        }

        override fun getTaskState(taskID: Long): Int {
            return getTaskState(m_tasksFactory, taskID)
        }

        override fun releaseTask(taskID: Long) {
            releaseTask(m_tasksFactory, taskID)
        }

        override fun cancelTask(taskID: Long) {
            cancelTask(m_tasksFactory, taskID)
        }

        override fun getProgress(taskID: Long): Float {
            return getProgress(m_tasksFactory, taskID)
        }

    }
    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG,"onDestroy executed")
        android.os.Process.killProcess(android.os.Process.myPid())
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
    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }
}