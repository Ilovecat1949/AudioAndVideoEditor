//
// Created by deng on 2024/11/16.
//

#include "FFmpegInfo.h"
extern "C" {
#include "libavcodec/avcodec.h"
}

char *FFmpegInfo::getStrCodecInfo() {
    char* info = new char[60000]();
    char mid_info[6][10000]={0};
    void *iter = NULL;
    const AVCodec *codec = NULL;
    codec = av_codec_iterate(&iter);
    while (codec) {
        char mid_info2[100]={0};
        int i=0;
        int j=0;
        if (av_codec_is_encoder(codec)){
            sprintf(mid_info2, "%sencode", mid_info2);
            i=0;
        }
        else{
            sprintf(mid_info2, "%sdecode", mid_info2);
            i=3;
        }
        switch (codec->type) {
            case AVMEDIA_TYPE_VIDEO:
                sprintf(mid_info2, "%s(video):", mid_info2);
                j=0;
                break;
            case AVMEDIA_TYPE_AUDIO:
                sprintf(mid_info2, "%s(audio):", mid_info2);
                j=1;
                break;
            default:
                sprintf(mid_info2, "%s(other):", mid_info2);
                j=2;
                break;
        }
        sprintf(mid_info2, "%s[%s]\n", mid_info2, codec->name);
        sprintf(mid_info[i+j], "%s%s", mid_info[i+j],mid_info2);
        codec = av_codec_iterate(&iter);
    }
    sprintf(info, "%s%s%s%s%s%s", mid_info[0],mid_info[1],mid_info[2],mid_info[3],mid_info[4],mid_info[5]);
    return info;
}
char *FFmpegInfo::getStrFFmpegInfo(int info_type) {
    if(info_type==0){
        return getStrCodecInfo();
    }
    return nullptr;
}
