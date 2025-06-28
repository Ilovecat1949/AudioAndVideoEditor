package com.example.audioandvideoeditor.viewmodel


import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.audioandvideoeditor.entity.MediaInfo
import com.example.audioandvideoeditor.services.TasksBinder

class VideoCropViewModel : ViewModel() {
    data class Rect(var start: Float, var top: Float, var width: Float, var height: Float)
    var video_rect by mutableStateOf(Rect(0f,0f,0f,0f,))
    var crop_rect by mutableStateOf(Rect(0f,0f,0f,0f))
    var crop_start by mutableStateOf(
            0f
        )
    var crop_top by  mutableStateOf(0f)
    var crop_width by  mutableStateOf(1f)
    var crop_height by mutableStateOf(1f)
    var density=0f
    val info= MediaInfo()
    var currentVideoUri by mutableStateOf<Uri?>(null)
        private set

    private var exoPlayer: ExoPlayer? = null

    fun setVideoUri(uri: Uri?) {
        currentVideoUri = uri
    }
    lateinit var tasksBinder: TasksBinder
    var initialize_source_flag by mutableStateOf(false)
    fun initializeSource(context: Context) {
        if(currentVideoUri != null) {
            val text = tasksBinder.getAVInfo(currentVideoUri!!.path!!)
            info.initInfo(text)
            videoDimensions=getNewVideoDimensions(screenWidth,screenHeight,info.width,info.height)
            video_rect=Rect((screenWidth-videoDimensions.first)/2f, (screenHeight-videoDimensions.second)/2f, videoDimensions.first*1f, videoDimensions.second*1f)
            crop_rect =Rect(video_rect.start+video_rect.width*1/3, video_rect.top+video_rect.height*1/3, video_rect.width*1/3, video_rect.height*1/3)
            crop_start=1f/3
            crop_top=1f/3
            crop_width=1f/3
            crop_height=1f/3
        }
        if (exoPlayer == null && currentVideoUri != null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(currentVideoUri!!)
                setMediaItem(mediaItem)
                prepare()
            }
        }
        initialize_source_flag=true
    }

    fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    fun getExoPlayer(): ExoPlayer? {
        return exoPlayer
    }

    fun getDuration(): Long{
        return exoPlayer?.duration?: 0L
    }
    var screenWidth: Int=0
    var screenHeight: Int=0
    var videoDimensions by mutableStateOf(Pair(0,0))
    fun getNewVideoDimensions(
        width:Int,
        heigth:Int,
        a:Int,
        b:Int
    ): Pair<Int, Int> {
        val ratio=a*1f/b
        val h_max = Math.min(heigth * 1f, width / ratio)
        val new_w = ratio * h_max
        return Pair(new_w.toInt(), h_max.toInt())
    }

    var editFileNameFlag = mutableStateOf(false)
    var target_name=""
}


//以下代码暂时不用
//    lateinit var exoPlayer :ExoPlayer
//    lateinit var  player :ExoPlayer
//    var isVideoLoaded by mutableStateOf(false)
//        private set
//    var videoLoadError by mutableStateOf<PlaybackException?>(null)
//        private set
//    fun initSource(context:Context,path:String) {
//        exoPlayer = ExoPlayer.Builder(context).build()
//        player = exoPlayer
//        val mediaItem = MediaItem.fromUri(Uri.parse(path))
//        exoPlayer.setMediaItem(mediaItem)
//        exoPlayer.prepare()
//        exoPlayer.playWhenReady = true
//        exoPlayer.addListener(object : Player.Listener {
//            override fun onPlaybackStateChanged(playbackState: Int) {
//                if (playbackState == Player.STATE_READY) {
//                    isVideoLoaded = true
//                    videoLoadError = null
//                }
//            }
//
//            override fun onPlayerError(error: PlaybackException) {
//                videoLoadError = error
//                isVideoLoaded = false
//            }
//
//            override fun onVideoSizeChanged(videoSize: VideoSize) {
//                super.onVideoSizeChanged(videoSize)
//            }
//
//        })
//    }
//
//    var cropRect by mutableStateOf(CropRect(0f, 0f, 1f, 1f)) // Normalized coordinates (0-1)
//        private set
//
//    fun updateCropRect(newRect: CropRect) {
//        cropRect = newRect
//    }
//
//    fun getActualCropCoordinates(videoWidth: Int, videoHeight: Int): CroppedArea {
//        val left = (cropRect.left * videoWidth).toInt()
//        val top = (cropRect.top * videoHeight).toInt()
//        val width = (cropRect.width * videoWidth).toInt()
//        val height = (cropRect.height * videoHeight).toInt()
//        return CroppedArea(left, top, width, height)
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        exoPlayer.release()
//    }
//    data class CropRect(var left: Float, var top: Float, var width: Float, var height: Float)
//    data class CroppedArea(val left: Int, val top: Int, val width: Int, val height: Int)


//**********************
//    private var _borderRect = mutableStateOf(Rect(100f, 100f, 300f, 300f))
//    val borderRect: State<Rect> = _borderRect
//    var borderRect by mutableStateOf(Rect(100f, 100f, 300f, 300f))
//
//    private var dragOffset = Offset.Zero
//    private var dragHandle: DragHandle? = null
//    private var lastDragHandle: DragHandle? = null
//
//    fun updateRect(rect: Rect, maxAreaSize: Size) {
//        val clampedRect = rect.clampToBounds(maxAreaSize,max_rect)
//        borderRect = clampedRect
//    }
//
//    fun startDrag(offset: Offset, maxAreaSize: Size) {
//        val rect = borderRect
//        dragHandle = getHandle(offset, rect)
//        dragOffset = offset
//    }
//
//    fun drag(offset: Offset, maxAreaSize: Size) {
//        val rect = borderRect
//        val delta = offset - dragOffset
//        dragOffset = offset
//
//        val newRect = when (dragHandle) {
//            DragHandle.TOP_LEFT -> rect.copy(left = rect.left + delta.x, top = rect.top + delta.y)
//            DragHandle.TOP_RIGHT -> rect.copy(right = rect.right + delta.x, top = rect.top + delta.y)
//            DragHandle.BOTTOM_LEFT -> rect.copy(left = rect.left + delta.x, bottom = rect.bottom + delta.y)
//            DragHandle.BOTTOM_RIGHT -> rect.copy(right = rect.right + delta.x, bottom = rect.bottom + delta.y)
//            DragHandle.CENTER -> rect.copy(left = rect.left + delta.x, top = rect.top + delta.y, right = rect.right + delta.x, bottom = rect.bottom + delta.y)
//            DragHandle.TOP -> rect.copy(top = rect.top + delta.y)
//            DragHandle.BOTTOM -> rect.copy(bottom = rect.bottom + delta.y)
//            DragHandle.LEFT -> rect.copy(left = rect.left + delta.x)
//            DragHandle.RIGHT -> rect.copy(right = rect.right + delta.x)
//            null -> rect
//        }
//        updateRect(newRect, maxAreaSize)
//    }
//
//    fun endDrag() {
//        dragHandle = null
//        lastDragHandle = null
//    }

//    data class Rect(var left: Float, var top: Float, var right: Float, var bottom: Float) {
//        fun clampToBounds(maxAreaSize: Size,max_rect:Rect): Rect {
//            if (right < left) {
//                right = left + 1
//            }
//            if (bottom < top) {
//                bottom = top + 1
//            }
//
//            var width = right - left
//            var height = bottom - top

//            val clampedLeft = left.coerceIn(0f, maxAreaSize.width - width)
//            val clampedTop = top.coerceIn(0f, maxAreaSize.height - height)
//
//            return Rect(
//                left = clampedLeft,
//                top = clampedTop,
//                right = (clampedLeft + width).coerceIn(0f, maxAreaSize.width),
//                bottom = (clampedTop + height).coerceIn(0f, maxAreaSize.height)
//            )
//            val clampedLeft = if(left<max_rect.left || left>max_rect.right) max_rect.left else left
//            val clampedTop = if(top<max_rect.top || top>max_rect.bottom) max_rect.top else top
//            val clampedRight = if(right<max_rect.left || right>max_rect.right) max_rect.right else right
//            val clampedBottom = if(bottom<max_rect.top || bottom>max_rect.bottom) max_rect.bottom else bottom
//            return Rect(
//                left = clampedLeft,
//                top = clampedTop,
//                right = clampedRight,
//                bottom = clampedBottom
//            )
//        }
//    }
//
//    enum class DragHandle {
//        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER, TOP, BOTTOM, LEFT, RIGHT
//    }
//
//    fun getHandle(offset: Offset, rect: Rect): DragHandle? {
//        val handleRadius = 5f
//        val handleDetectionRadius = 50f // Increased detection radius
//        val edgeHandleWidth = 40f // Increased edge handle width
//        val edgeBuffer = 10f
//
//        val handles = listOf(
//            DragHandle.TOP_LEFT to Offset(rect.left, rect.top),
//            DragHandle.TOP_RIGHT to Offset(rect.right, rect.top),
//            DragHandle.BOTTOM_LEFT to Offset(rect.left, rect.bottom),
//            DragHandle.BOTTOM_RIGHT to Offset(rect.right, rect.bottom),
//            DragHandle.CENTER to Offset((rect.left + rect.right) / 2, (rect.top + rect.bottom) / 2)
//        )
//
//        for ((handle, handleOffset) in handles) {
//            if ((offset - handleOffset).getDistanceSquared() < handleDetectionRadius * handleDetectionRadius) {
//                lastDragHandle = handle
//                return handle
//            }
//        }
//
//        if (offset.y in rect.top - edgeHandleWidth - edgeBuffer..rect.top + edgeHandleWidth + edgeBuffer && offset.x in rect.left..rect.right) {lastDragHandle = DragHandle.TOP; return DragHandle.TOP}
//        if (offset.y in rect.bottom - edgeHandleWidth - edgeBuffer..rect.bottom + edgeHandleWidth + edgeBuffer && offset.x in rect.left..rect.right) {lastDragHandle = DragHandle.BOTTOM; return DragHandle.BOTTOM}
//        if (offset.x in rect.left - edgeHandleWidth - edgeBuffer..rect.left + edgeHandleWidth + edgeBuffer && offset.y in rect.top..rect.bottom) {lastDragHandle = DragHandle.LEFT; return DragHandle.LEFT}
//        if (offset.x in rect.right - edgeHandleWidth - edgeBuffer..rect.right + edgeHandleWidth + edgeBuffer && offset.y in rect.top..rect.bottom) {lastDragHandle = DragHandle.RIGHT; return DragHandle.RIGHT}
//
//        return if (offset.x > rect.left && offset.x < rect.right && offset.y > rect.top && offset.y < rect.bottom) {lastDragHandle = DragHandle.CENTER; DragHandle.CENTER} else lastDragHandle
//    }
//
//    suspend fun debouncedDrag(offset: Offset, maxAreaSize: Size) {
//        delay(16)
//        drag(offset, maxAreaSize)
//    }
//**********************