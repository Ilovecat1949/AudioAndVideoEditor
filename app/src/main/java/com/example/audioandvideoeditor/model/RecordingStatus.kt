package com.example.audioandvideoeditor.model

/**
 * 屏幕录制业务的全局生命周期状态
 * 属于领域层（Repository）的统一业务语言，彻底与 Service 的底层实现解耦
 */
enum class RecordingStatus {
    IDLE,          // 闲置状态 / 未开始录制
    PENDING,       // 启动中：用户刚点了开始，前台正在等待系统权限弹窗允许（此时UI可展示转圈，防重复点击）
    RECORDING,     // 正在录制中
    PAUSED     ,    // 已暂停
    STOPPED     ,// 已结束
    SAVING      ,// 正在保存数据到数据库（对应刚才的安全退出状态）
}