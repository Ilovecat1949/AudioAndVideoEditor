package com.example.audioandvideoeditor.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.audioandvideoeditor.entity.Task

@Database(version = 1, entities = [Task::class])
abstract class AppDatabase : RoomDatabase(){
    abstract fun taskDao():TasksDao
    companion object{
        private var instance:AppDatabase?=null
        @Synchronized
        fun getDatabase(context: Context):AppDatabase{
            instance?.let {
                return it
            }
            return Room.databaseBuilder(context.applicationContext,
                AppDatabase::class.java,"app_database")
                .build().apply {
                    instance=this
                }
        }
    }

}