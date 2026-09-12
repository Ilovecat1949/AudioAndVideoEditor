package com.example.audioandvideoeditor.transcoder

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.example.audioandvideoeditor.model.TranscodeTaskConfig
import java.io.File

/**
 * 硬件转码总控引擎
 * 负责调度视频/音频轨 Processing 管道、协调 Muxer 启动，并实时回调进度与状态
 */
class HardwareTranscoder(
    private val taskConfig: TranscodeTaskConfig,
    private val listener: ITranscodeListener
) {

    @Volatile
    private var isCanceled = false

    /**
     * 执行转码任务（建议在后台子线程中调用）
     *
     * @param inputPath 输入视频文件路径
     * @param outputPath 目标输出视频文件路径 (.mp4)
     */
    fun startTranscode(inputPath: String, outputPath: String) {
        val inputFile = File(inputPath)
        if (!inputFile.exists()) {
            listener.onError(IllegalArgumentException("Input file does not exist: $inputPath"))
            return
        }

        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null

        var videoProcessor: VideoTrackProcessor? = null
        var audioProcessor: AudioTrackProcessor? = null

        try {
            listener.onStart()

            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var videoFormat: MediaFormat? = null
            var audioFormat: MediaFormat? = null

            // 1. 初始化 Extractor 并探测音视频轨道
            val extractor = MediaExtractor().apply { setDataSource(inputPath) }
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") && videoTrackIndex < 0 && taskConfig.videoConfig != null) {
                    videoTrackIndex = i
                    videoFormat = format
                } else if (mime.startsWith("audio/") && audioTrackIndex < 0 && taskConfig.audioConfig != null) {
                    audioTrackIndex = i
                    audioFormat = format
                }
            }
            extractor.release()

            if (videoTrackIndex < 0 && audioTrackIndex < 0) {
                throw IllegalArgumentException("No valid video or audio tracks found in source file")
            }

            // 2. 准备视频轨 Processor
            if (videoTrackIndex >= 0 && videoFormat != null) {
                videoExtractor = MediaExtractor().apply {
                    setDataSource(inputPath)
                    selectTrack(videoTrackIndex)
                }
                videoProcessor = VideoTrackProcessor(taskConfig.videoConfig!!).apply {
                    prepare(videoExtractor, videoTrackIndex, videoFormat)
                }
            }

            // 3. 准备音频轨 Processor
            if (audioTrackIndex >= 0 && audioFormat != null) {
                audioExtractor = MediaExtractor().apply {
                    setDataSource(inputPath)
                    selectTrack(audioTrackIndex)
                }
                audioProcessor = AudioTrackProcessor(taskConfig.audioConfig!!).apply {
                    prepare(audioFormat)
                }
            }

            // 4. 初始化 Muxer
            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // 计算总时长 (微秒) 用于计算百分比进度
            val videoDurationUs = videoFormat?.let {
                if (it.containsKey(MediaFormat.KEY_DURATION)) it.getLong(MediaFormat.KEY_DURATION) else 0L
            } ?: 0L
            val audioDurationUs = audioFormat?.let {
                if (it.containsKey(MediaFormat.KEY_DURATION)) it.getLong(MediaFormat.KEY_DURATION) else 0L
            } ?: 0L
            val totalDurationUs = maxOf(videoDurationUs, audioDurationUs)

            // Muxer 多轨延迟启动逻辑
            val expectedTrackCount = (if (videoProcessor != null) 1 else 0) + (if (audioProcessor != null) 1 else 0)
            var addedTrackCount = 0
            var isMuxerStarted = false

            val onMuxerStartCheck: (MediaFormat) -> Int = { trackFormat ->
                synchronized(muxer) {
                    val trackIdx = muxer.addTrack(trackFormat)
                    addedTrackCount++
                    if (addedTrackCount >= expectedTrackCount) {
                        muxer.start()
                        isMuxerStarted = true
                    }
                    trackIdx
                }
            }

            var videoDone = videoProcessor == null
            var audioDone = audioProcessor == null

            var currentVideoPtsUs = 0L
            var currentAudioPtsUs = 0L

            // 5. 核心驱动循环 (Drain Loop)
            while ((!videoDone || !audioDone) && !isCanceled) {
                // 处理视频帧
                if (!videoDone) {
                    val pts = videoProcessor!!.processFrame(videoExtractor!!, videoTrackIndex, muxer, onMuxerStartCheck)
                    if (pts < 0) {
                        videoDone = true
                    } else {
                        currentVideoPtsUs = pts
                    }
                }

                // 处理音频帧
                if (!audioDone) {
                    val pts = audioProcessor!!.processFrame(audioExtractor!!, audioTrackIndex, muxer, onMuxerStartCheck)
                    if (pts < 0) {
                        audioDone = true
                    } else {
                        currentAudioPtsUs = pts
                    }
                }

                // 回调计算进度
                if (totalDurationUs > 0) {
                    val maxProcessedPtsUs = maxOf(currentVideoPtsUs, currentAudioPtsUs)
                    val progress = (maxProcessedPtsUs.toFloat() / totalDurationUs.toFloat() * 100f).coerceIn(0f, 100f)
                    listener.onProgress(progress)
                }
            }

            if (isCanceled) {
                File(outputPath).delete()
                listener.onCanceled()
            } else {
                listener.onProgress(100f)
                listener.onSuccess(outputPath)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Transcoding failed", e)
            File(outputPath).delete()
            listener.onError(e)
        } finally {
            // 资源清理
            runCatching { videoProcessor?.release() }
            runCatching { audioProcessor?.release() }
            runCatching { videoExtractor?.release() }
            runCatching { audioExtractor?.release() }
            runCatching {
                muxer?.stop()
                muxer?.release()
            }
        }
    }

    /**
     * 取消当前转码任务
     */
    fun cancel() {
        isCanceled = true
    }

    companion object {
        private const val TAG = "HardwareTranscoder"
    }
}