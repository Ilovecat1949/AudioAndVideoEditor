package com.example.audioandvideoeditor.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Task(
    @PrimaryKey
    var task_id:Long,
    var type:Int,
    var status:Int,
    var path:String,
    var log_path:String,
    var date:String
)
