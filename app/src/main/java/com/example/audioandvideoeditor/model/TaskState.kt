package com.example.audioandvideoeditor.model

enum class TaskState(val code: Int) {
    UNKNOWN(-2),
    FAILED(-1),
    UNFINISHED(0),
    SUCCESS(1),
    CANCELED(2),
    IDLE(3),
    INITED(4),
    RUNNING(5),
}