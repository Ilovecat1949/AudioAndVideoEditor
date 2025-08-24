package com.example.audioandvideoeditor.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.audioandvideoeditor.entity.MediaInfo
import com.example.audioandvideoeditor.services.TasksBinder
import com.example.audioandvideoeditor.utils.FilesUtils
import com.example.audioandvideoeditor.utils.TextsUtils
import kotlin.math.roundToInt
import kotlin.math.sqrt
class VideoCompressViewModel: ViewModel() {
    val info= MediaInfo()
    var videoSize=0L
    lateinit var tasksBinder: TasksBinder
    var currentVideoUri by mutableStateOf<Uri?>(null)
        private set
    private var exoPlayer: ExoPlayer? = null
    private var compressionRatio=
        listOf(0.95,0.9,0.85,0.8,0.75,0.7,0.65,0.6,0.55,0.5,0.45,0.4,0.35,0.3,0.25)
    lateinit var option:Triple<Double,Pair<Int,Int>,String>
    var compressionOptions=ArrayList<Triple<Double,Pair<Int,Int>,String>>()
    fun setVideoUri(uri: Uri?) {
        currentVideoUri = uri
    }
    var initialize_source_flag by mutableStateOf(false)
    fun initializeSource(context:Context) {
        if(currentVideoUri != null) {
            val text = tasksBinder.getAVInfo(currentVideoUri!!.path!!)
            info.initInfo(text)

        }
        if (exoPlayer == null && currentVideoUri != null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(currentVideoUri!!)
                setMediaItem(mediaItem)
                prepare()
            }
        }
        compressionOptions.clear()
        compressionRatio.forEach {
            val dimensions=calculateCompressedDimensions(info.width,info.height,it)
            if(dimensions!=null && dimensions.first>=75 && dimensions.second>=75){
                compressionOptions.add(Triple(it,dimensions, TextsUtils.getSizeText((videoSize*it).toLong())))
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
    var editFileNameFlag = mutableStateOf(false)
    var target_name=""
    /**
     * Calculates the compressed video dimensions (width and height) based on
     * the original resolution and a desired compression ratio.
     *
     * This algorithm ensures that the output width is a multiple of 2 and
     * the output height is a multiple of 16, while trying to achieve a
     * compression ratio as close as possible to the desired one.
     *
     * @param originalWidth The original width of the video.
     * @param originalHeight The original height of the video.
     * @param compressionRatio The desired compression ratio (e.g., 0.9 for 90% of original, 0.5 for 50%).
     * Must be between 0.0 (exclusive) and 1.0 (inclusive).
     * @return A Pair<Int, Int> representing the compressed width and height, or null if input is invalid.
     */
    private fun calculateCompressedDimensions(
        originalWidth: Int,
        originalHeight: Int,
        compressionRatio: Double
    ): Pair<Int, Int>? {
        // 1. Input Validation and Error Handling
        if (originalWidth <= 0 || originalHeight <= 0) {
            println("Error: Original width and height must be positive integers.")
            return null
        }
        if (compressionRatio <= 0.0 || compressionRatio > 1.0) {
            println("Error: Compression ratio must be between 0.0 (exclusive) and 1.0 (inclusive).")
            return null
        }

        val originalArea = (originalWidth * originalHeight).toDouble()
        val targetArea = originalArea * compressionRatio

        // 2. Calculate Initial Scaled Dimensions based on Target Area
        // Maintain aspect ratio: new_width / new_height = original_width / original_height
        // new_width = original_width * (new_height / original_height)
        // new_width * new_height = target_area
        // (original_width * (new_height / original_height)) * new_height = target_area
        // new_height^2 = target_area * original_height / original_width
        // new_height = sqrt(target_area * original_height / original_width)

        val aspectRatio = originalWidth.toDouble() / originalHeight.toDouble()

        val estimatedHeight = sqrt(targetArea / aspectRatio).roundToInt()
        val estimatedWidth = (estimatedHeight * aspectRatio).roundToInt()

        // 3. Adjust to meet "multiple of" constraints (prioritize height for 16)
        // We'll iterate to find the closest valid dimensions.
        // Start by adjusting the height, then derive width to maintain aspect ratio,
        // and finally adjust width to be a multiple of 2.

        var newWidth = estimatedWidth
        var newHeight = estimatedHeight

        // Adjust height to be a multiple of 16
        // We try to find the closest multiple of 16.
        val remainderHeight = newHeight % 16
        if (remainderHeight != 0) {
            val lowerMultiple = newHeight - remainderHeight
            val upperMultiple = newHeight + (16 - remainderHeight)

            // Choose the multiple closest to the estimated height
            newHeight = if (newHeight - lowerMultiple <= upperMultiple - newHeight) {
                lowerMultiple
            } else {
                upperMultiple
            }
            // Ensure minimum of 16 if very small
            if (newHeight == 0) newHeight = 16
        }

        // Adjust width based on the new height to maintain aspect ratio as much as possible
        newWidth = (newHeight * aspectRatio).roundToInt()

        // Adjust width to be a multiple of 2
        val remainderWidth = newWidth % 2
        if (remainderWidth != 0) {
            val lowerMultiple = newWidth - remainderWidth
            val upperMultiple = newWidth + (2 - remainderWidth)

            // Choose the multiple closest to the derived width
            newWidth = if (newWidth - lowerMultiple <= upperMultiple - newWidth) {
                lowerMultiple
            } else {
                upperMultiple
            }
            // Ensure minimum of 2 if very small
            if (newWidth == 0) newWidth = 2
        }

        // 4. Final Sanity Check: Ensure dimensions are not 0 and maintain aspect ratio roughly
        // It's possible for very aggressive compression ratios on small videos that dimensions might become too small.
        // Ensure minimum valid dimensions
        if (newWidth < 2) newWidth = 2
        if (newHeight < 16) newHeight = 16

        // Recalculate width if height was adjusted to minimum
        if (newHeight == 16 && (newWidth.toDouble() / newHeight.toDouble() != aspectRatio)) {
            newWidth = (16 * aspectRatio).roundToInt()
            if (newWidth % 2 != 0) {
                newWidth = (newWidth / 2) * 2
                if (newWidth == 0) newWidth = 2
            }
        }
        // Recalculate height if width was adjusted to minimum
        if (newWidth == 2 && (newWidth.toDouble() / newHeight.toDouble() != aspectRatio)) {
            newHeight = (2 / aspectRatio).roundToInt()
            if (newHeight % 16 != 0) {
                newHeight = (newHeight / 16) * 16
                if (newHeight == 0) newHeight = 16
            }
        }


        // 5. Calculate Actual Compression Ratio for verification (optional, for logging/debugging)
        val actualArea = (newWidth * newHeight).toDouble()
        val actualCompressionRatio = actualArea / originalArea
        //println("Calculated dimensions: ${newWidth}x${newHeight}. Actual Compression Ratio: ${String.format("%.4f", actualCompressionRatio)}")

        return Pair(newWidth, newHeight)
    }
}