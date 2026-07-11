package com.example.audioandvideoeditor.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.audioandvideoeditor.entity.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TasksDao {
    @Query("SELECT MAX(task_id) FROM task")
    suspend fun getMaxTaskId(): Long?

    @Insert
    suspend fun insertTask(task: Task)

    @Query("SELECT * FROM task ORDER BY date DESC")
    suspend fun getAllTasks(): List<Task>?

    @Query("SELECT * FROM task WHERE task_id = :taskId")
    suspend fun getTaskById(taskId: Long): Task?

    @Query("UPDATE task SET status = :newStatus WHERE task_id = :taskId")
    suspend fun updateTaskStatus(taskId: Long, newStatus: Int)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM task")
    suspend fun deleteAllTasks()

    @Query("SELECT * FROM task WHERE status = :status")
    suspend fun getTasksByStatus(status: Int): List<Task>?

    @Query("select * from Task where type=(:type) order by task_id desc")
    fun loadTasksByType(type:Int): Flow<List<Task>>
}