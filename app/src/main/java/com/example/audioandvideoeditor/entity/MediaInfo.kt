package com.example.audioandvideoeditor.entity

class MediaInfo {
    fun initInfo(info:String){
        val info2=info.split('\n')
        info2.forEach {
            val info3=it.split(':')
            if(info3.size>1){
                infoMap[info3[0]]= info3[1]
            }
        }
        if(infoMap.containsKey("Width")){
            width=infoMap["Width"]!!.toInt()
        }
        if(infoMap.containsKey("Height")){
            height=infoMap["Height"]!!.toInt()
        }
        if(infoMap.containsKey("FrameRate")){
            frame_rate=infoMap["FrameRate"]!!.toFloat()
        }
        if(infoMap.containsKey("VideoBitRate")){
            video_bit_rate=infoMap["VideoBitRate"]!!.toLong()
        }
        if(infoMap.containsKey("Duration")){
            duration=infoMap["Duration"]!!.toLong()
        }
        if(infoMap.containsKey("SampleRate")){
            sample_rate=infoMap["SampleRate"]!!.toInt()
        }
        if(infoMap.containsKey("Channels")){
            channels=infoMap["Channels"]!!.toInt()
        }
        if(infoMap.containsKey("audioBitRate")){
            audio_bit_rate=infoMap["audioBitRate"]!!.toLong()
        }
    }
    val infoMap=HashMap<String,String>()
    var width=-1
        private set
    var height=-1
        private set
    var frame_rate=-1f
        private set
    var video_bit_rate=-1L
        private set
    var duration=-1L
    var sample_rate=-1
        private set
    var channels=-1
        private set
    var audio_bit_rate=-1L
        private set
}