package com.example.audioandvideoeditor.model

enum class TaskType(val code: Int) {
    REENCODING_TASK(0),
    REPACKAGING_TASK(1),
    FFMPEGCOMMANDS_TASK(2),
    FFMPEGSERVICE_TASK(3),
    RECORDING_TASK(4),
    HARDWARETRANSCODE_TASK(5)
}