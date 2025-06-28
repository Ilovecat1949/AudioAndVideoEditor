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
        if(infoMap.containsKey("width")){
            width=infoMap["width"]!!.toInt()
        }
        if(infoMap.containsKey("height")){
            height=infoMap["height"]!!.toInt()
        }
        if(infoMap.containsKey("frame_rate")){
            frame_rate=infoMap["frame_rate"]!!.toFloat()
        }
        if(infoMap.containsKey("video_bit_rate")){
            video_bit_rate=infoMap["video_bit_rate"]!!.toLong()
        }
//        if(infoMap.containsKey("Duration")){
//            duration=infoMap["Duration"]!!.toLong()
//        }
        if(infoMap.containsKey("video_duration")){
            video_duration=infoMap["video_duration"]!!.toLong()
        }
        if(infoMap.containsKey("audio_duration")){
            audio_duration=infoMap["audio_duration"]!!.toLong()
        }
        if(infoMap.containsKey("sample_rate")){
            sample_rate=infoMap["sample_rate"]!!.toInt()
        }
        if(infoMap.containsKey("channels")){
            channels=infoMap["channels"]!!.toInt()
        }
        if(infoMap.containsKey("audio_bit_rate")){
            audio_bit_rate=infoMap["audio_bit_rate"]!!.toLong()
        }
        if(infoMap.containsKey("audio_codec_type")){
            audio_codec_type=infoMap["audio_codec_type"]!!
        }
        if(infoMap.containsKey("video_codec_type")){
            video_codec_type=infoMap["video_codec_type"]!!
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
    var video_duration=-1L
    private set
    var audio_duration=-1L
        private set
    var sample_rate=-1
        private set
    var channels=-1
        private set
    var audio_bit_rate=-1L
        private set
    var audio_codec_type=""
        private set
    var video_codec_type=""
        private set
}