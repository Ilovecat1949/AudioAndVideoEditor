package com.example.audioandvideoeditor.transcoder.core

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.example.audioandvideoeditor.model.TranscodeTaskConfig
import com.example.audioandvideoeditor.transcoder.track.AudioTrackProcessor
import com.example.audioandvideoeditor.transcoder.infrastructure.MuxerWrapper
import com.example.audioandvideoeditor.transcoder.track.PassthroughTrackProcessor
import com.example.audioandvideoeditor.transcoder.track.VideoTrackProcessor
import java.io.File

/**
 * 硬件转码总控引擎（高鲁棒性、无锁交错调度版）
 * 支持全轨道（视频、音频、字幕及其他辅助轨）安全调度与透明复制
 */
class HardwareTranscoder(
    private val taskConfig: TranscodeTaskConfig,
    private val listener: ITranscodeListener
) {

    @Volatile
    private var isCanceled = false

    private class OtherTrackInfo(
        val trackIndex: Int,
        val extractor: MediaExtractor,
        val processor: PassthroughTrackProcessor
    )

    fun startTranscode(inputPath: String, outputPath: String) {
        val inputFile = File(inputPath)
        if (!inputFile.exists()) {
            listener.onError(IllegalArgumentException("Input file does not exist: $inputPath"))
            return
        }

        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        val otherTrackInfos = mutableListOf<OtherTrackInfo>()
        var muxerWrapper: MuxerWrapper? = null

        var videoProcessor: VideoTrackProcessor? = null
        var audioProcessor: AudioTrackProcessor? = null

        try {
            listener.onStart()

            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var videoFormat: MediaFormat? = null
            var audioFormat: MediaFormat? = null

            val otherTrackIndices = mutableListOf<Int>()

            // 1. 初始化 Extractor 并分类探测所有轨道
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
                } else {
                    otherTrackIndices.add(i)
                }
            }
            extractor.release()

            if (videoTrackIndex < 0 && audioTrackIndex < 0 && otherTrackIndices.isEmpty()) {
                throw IllegalArgumentException("No valid readable tracks found in source file")
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

            // 4. 准备其他辅助轨道 Processor
            for (trackIdx in otherTrackIndices) {
                val otherExtractor = MediaExtractor().apply {
                    setDataSource(inputPath)
                    selectTrack(trackIdx)
                }
                otherTrackInfos.add(
                    OtherTrackInfo(
                        trackIndex = trackIdx,
                        extractor = otherExtractor,
                        processor = PassthroughTrackProcessor()
                    )
                )
            }

            // 5. 初始化 MuxerWrapper
            val expectedTrackCount = (if (videoProcessor != null) 1 else 0) +
                    (if (audioProcessor != null) 1 else 0) +
                    otherTrackInfos.size

            val rawMuxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxerWrapper = MuxerWrapper(rawMuxer, expectedTrackCount)

            // 计算进度参考总时长 (Us)
            val videoDurationUs = videoFormat?.let {
                if (it.containsKey(MediaFormat.KEY_DURATION)) it.getLong(MediaFormat.KEY_DURATION) else 0L
            } ?: 0L
            val audioDurationUs = audioFormat?.let {
                if (it.containsKey(MediaFormat.KEY_DURATION)) it.getLong(MediaFormat.KEY_DURATION) else 0L
            } ?: 0L
            val totalDurationUs = maxOf(videoDurationUs, audioDurationUs)

            var currentVideoPtsUs = 0L
            var currentAudioPtsUs = 0L

            var progress=0f

            // 6. 核心驱动循环 (Interleaved Drain Loop)
            while (!isCanceled) {
                val videoCompleted = videoProcessor?.isCompleted() ?: true
                val audioCompleted = audioProcessor?.isCompleted() ?: true
                val otherTracksCompleted = otherTrackInfos.all { it.processor.isDone() }
                // 两轨均已彻底完成，直接退出
                if (videoCompleted && audioCompleted && otherTracksCompleted) {
                    break
                }

                var stepProcessed = false

                // PTS 步进交错控制：优先推进时间戳较滞后的一轨，保证 Muxer 数据交错平衡
                val shouldStepVideo = !videoCompleted && (audioCompleted || currentVideoPtsUs <= currentAudioPtsUs)
                val shouldStepAudio = !audioCompleted && (videoCompleted || currentAudioPtsUs < currentVideoPtsUs)

                if (shouldStepVideo) {
                    val pts = videoProcessor!!.processFrame(videoExtractor!!, videoTrackIndex, muxerWrapper)
                    if (pts >= 0L) { // 修正 1：允许 PTS == 0L 的有效帧通过，避免开局挂起
                        currentVideoPtsUs = pts
                        stepProcessed = true
                    }
                }

                if (shouldStepAudio) {
                    val pts = audioProcessor!!.processFrame(audioExtractor!!, audioTrackIndex, muxerWrapper)
                    if (pts >= 0L) { // 修正 2：允许 PTS == 0L 的有效帧通过
                        currentAudioPtsUs = pts
                        stepProcessed = true
                    }
                }

                // 顺带拉取辅助轨道帧（字幕等），附带 safe guard 异常隔离
                for (info in otherTrackInfos) {
                    runCatching {
                        if (!info.processor.isDone()) {
                            info.processor.processFrame(info.extractor, info.trackIndex, muxerWrapper)
                        }
                    }
                }

                // 若本轮无任何有效 Sample 产出，短暂挂起释放 CPU 调度权
                if (!stepProcessed) {
                    runCatching { Thread.sleep(2) }
                }

                // 平滑计算进度：取音视频较慢一轨的 PTS（避免进度条提前假跳）
                if (totalDurationUs > 0) {
                    val processedPts = when {
                        !videoCompleted && !audioCompleted -> minOf(currentVideoPtsUs, currentAudioPtsUs)
                        !videoCompleted -> currentVideoPtsUs
                        else -> currentAudioPtsUs
                    }
                    val p = (processedPts.toFloat() / totalDurationUs.toFloat() ).coerceIn(0f, 0.9999f)
                    if(p>progress){
                        progress=p
                    }
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

            for (info in otherTrackInfos) {
                runCatching { info.processor.release() }
                runCatching { info.extractor.release() }
            }

            muxerWrapper?.safeRelease()
        }
    }

    fun cancel() {
        isCanceled = true
    }

    companion object {
        private const val TAG = "HardwareTranscoder"
    }
}