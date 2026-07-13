package com.example.audioandvideoeditor.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.audioandvideoeditor.entity.Task

@Database(version = 3, entities = [Task::class])
abstract class AppDatabase : RoomDatabase(){
    abstract fun taskDao():TasksDao
    companion object{
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("alter table Task add column uri text")
//                db.execSQL("alter table Task add column file_name text")
            }
        }
        // 🌟 核心救场逻辑
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 探测本地表，自动分流处理
                val cursor = db.query("PRAGMA table_info(Task)")
                var hasFileNameColumn = false
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex != -1) {
                    while (cursor.moveToNext()) {
                        if (cursor.getString(nameIndex) == "file_name") {
                            hasFileNameColumn = true
                            break
                        }
                    }
                }
                cursor.close()

                if (!hasFileNameColumn) {
                    // 老用户升级上来的，帮他补齐字段，原地复活
                    db.execSQL("alter table Task add column file_name text not null default ''")
                } // 新用户表里自带字段，走到 else，安全跳过，什么都不用做
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().apply {
                    instance=this
                }
        }
    }
}