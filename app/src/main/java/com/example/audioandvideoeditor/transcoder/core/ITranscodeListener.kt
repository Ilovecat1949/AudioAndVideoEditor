package com.example.audioandvideoeditor.transcoder.core

/**
 * 硬件转码进度与状态回调接口
 */
interface ITranscodeListener {
    /** 开始转码 */
    fun onStart()

    /**
     * 进度更新回调
     * @param progress 百分比进度 (0.0f - 100.0f)
     */
    fun onProgress(progress: Float)

    /** 转码成功完成 */
    fun onSuccess(outputPath: String)

    /** 转码过程抛出异常 */
    fun onError(exception: Exception)

    /** 用户主动取消 */
    fun onCanceled()
}