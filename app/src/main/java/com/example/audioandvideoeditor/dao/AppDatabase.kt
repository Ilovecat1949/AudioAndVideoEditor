package com.example.audioandvideoeditor.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.audioandvideoeditor.entity.Task

@Database(version = 2, entities = [Task::class])
abstract class AppDatabase : RoomDatabase(){
    abstract fun taskDao():TasksDao
    companion object{
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("alter table Task add column uri text")
            }
        }

        private var instance:AppDatabase?=null
        @Synchronized
        fun getDatabase(context: Context):AppDatabase{
            instance?.let {
                return it
            }
            return Room.databaseBuilder(context.applicationContext,
                AppDatabase::class.java,"app_database")
                .addMigrations(MIGRATION_1_2)
                .build().apply {
                    instance=this
                }
        }
    }
}