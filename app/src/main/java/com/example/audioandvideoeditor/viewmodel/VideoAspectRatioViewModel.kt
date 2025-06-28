package com.example.audioandvideoeditor.viewmodel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.example.audioandvideoeditor.MainActivity
import com.example.audioandvideoeditor.entity.MediaInfo
import kotlinx.coroutines.launch
import java.lang.Math.min

class VideoAspectRatioViewModel: ViewModel() {
    private  val TAG="VideoAspectRatioViewModel"
    var mediaItem = mutableStateOf<MediaItem?>(null)
//        private set
    var videoScreenWidth: Int=0
    var videoScreenHeight: Int=0
    var originalVideoWidth = mutableStateOf(0)
        private set
    var originalVideoHeight = mutableStateOf(0)
        private set

//    var selectedAspectRatio = mutableStateOf(AspectRatio.NINE_TO_SIXTEEN)
//        private set
    var selectedAspectRatio = mutableStateOf(Pair(1,1))
//        private set
    var selectedAspectIndex by mutableStateOf(0)

    var selectedBackgroundColor = mutableStateOf(Color.Black)
        private set
    var dimensionsReady = mutableStateOf(false)
        private set
    val info= MediaInfo()
    fun initSource(path:String, activity: MainActivity) {
//        originalVideoWidth.value = width
//        originalVideoHeight.value = height
        viewModelScope.launch{
            val text = activity.tasksBinder.getAVInfo(path)
            info.initInfo(text)
            originalVideoWidth.value = info.width
            originalVideoHeight.value = info.height
            dimensionsReady.value=true
            selectedAspectRatio.value=Pair(info.width,info.height)
            AspectRatio[0]=Pair(info.width,info.height)
            AspectRatio[2]=Pair(info.width,info.height)
        }
    }

    var editAspectRatioFlag by mutableStateOf(false)


    fun setAspectRatio(aspectRatio: Pair<Int,Int>) {
        selectedAspectRatio.value = aspectRatio
    }

    fun setBackgroundColor(color: Color) {
        selectedBackgroundColor.value = color
    }
val AspectRatio= mutableListOf(
    Pair(0,0),
    Pair(0,0),
    Pair(0,0),
    Pair(9,16),
    Pair(16,9),
    Pair(1,1),
    Pair(3,4),
    Pair(6,7),
    Pair(21,9),
    Pair(9,21),
)
//    enum class AspectRatio(val ratio: Pair<Int, Int>) {
//        NINE_TO_SIXTEEN(9 to 16),
//        SIXTEEN_TO_NINE(16 to 9),
//        ONE_TO_ONE(1 to 1),
//        THREE_TO_FOUR(3 to 4),
//        SIX_TO_SENVEN(6 to 7),
//        TWENTY_ONE_TO_NINE(21 to 9),
//        NINE_TO_TWENTY_ONE(9 to 21),
//        ;
//    }
    val colorList= listOf(
        Color(0xFF000000),Color(0xFFFFFFFF),Color(0xFFFF0000),Color(0xFF00FF00),
        Color(0xFF0000FF)
    )
    fun colorToString(
        color: Color
    ):String
    {
        return when(color){
            Color(0xFF000000)->"#000000"
            Color(0xFFFFFFFF)->"#FFFFFF"
            Color(0xFFFF0000)->"#FF0000"
            Color(0xFF00FF00)->"#00FF00"
            Color(0xFF0000FF)->"#0000FF"
            else ->""
        }
    }

    fun getNewVideoDimensions(): Pair<Int, Int> {
        val ratio=selectedAspectRatio.value.first*1f/selectedAspectRatio.value.second
        val h_max = min(videoScreenHeight*1f, videoScreenWidth / ratio)
        val new_w = ratio * h_max
        return Pair(new_w.toInt(), h_max.toInt())
    }
    fun getNewResolution(
        w:Int,
        h:Int,
        a:Int,
        b:Int
    ):Pair<Int, Int> {
        var new_w=0
        var new_h=0
        if (w * b >= h * a) {
            new_w = w
            new_h = w * b / a
        }
        else{
        new_h = h
        new_w = h * a / b
    }
        return Pair(new_w, new_h)
    }

    var editFileNameFlag = mutableStateOf(false)
    var target_name=""

}