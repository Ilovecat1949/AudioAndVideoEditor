package com.example.audioandvideoeditor.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.audioandvideoeditor.entity.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TasksDao {
    @Insert
    fun insertTask(task: Task)
    @Query("select count(1) from Task ")
    fun getTaskNum():Long
    @Query("select * from Task order by task_id desc")
    fun loadAllTasks():List<Task>
    @Query("select * from Task where status=(:status) order by task_id desc")
    fun loadTasksForStatus(status:Int):List<Task>
    @Delete
    fun deleteTask(task:Task)
    @Query("select max(task_id) from Task ")
    fun getMaxTaskId():Long?
    @Query("select * from Task where type=(:type) order by task_id desc")
    fun loadTasksByType(type:Int): Flow<List<Task>>
}