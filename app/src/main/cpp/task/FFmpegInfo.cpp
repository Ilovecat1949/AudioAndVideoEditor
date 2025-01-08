//
// Created by deng on 2024/11/16.
//

#include "FFmpegInfo.h"
extern "C" {
#include "libavcodec/avcodec.h"
}

char *FFmpegInfo::getStrCodecInfo() {
    char* info = new char[40000];
    for(int i=0;i<40000;i++){
        info[i]=0;
    }
    void *iter = NULL;
    const AVCodec *codec = NULL;
    codec = av_codec_iterate(&iter);
    while (codec) {
        if (av_codec_is_encoder(codec)){
            sprintf(info, "%sencode", info);
        }
        else{
            sprintf(info, "%sdecode", info);
        }
        switch (codec->type) {
            case AVMEDIA_TYPE_VIDEO:
                sprintf(info, "%s(video):", info);
                break;
            case AVMEDIA_TYPE_AUDIO:
                sprintf(info, "%s(audio):", info);
                break;
            default:
                sprintf(info, "%s(other):", info);
                break;
        }
        sprintf(info, "%s[%s]\n", info, codec->name);
        codec = av_codec_iterate(&iter);
    }
    return info;
}
char *FFmpegInfo::getStrFFmpegInfo(int info_type) {
    if(info_type==0){
        return getStrCodecInfo();
    }
    return nullptr;
}
