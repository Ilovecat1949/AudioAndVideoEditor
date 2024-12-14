//
// Created by deng on 2024/11/4.
//

#include "AudioAndVideoInfo.h"
#include "../utils/logger.h"

char *AudioAndVideoInfo::getStrInfo() {
    char* info = new char[40000];
    for(int i=0;i<40000;i++){
        info[i]=0;
    }
    if(video_stream_index!=-1){
//        sprintf(info, "%sMediaType:Video\n", info);
        sprintf(info, "%sWidth:%d\n", info,m_format_ctx->streams[video_stream_index]->codecpar->width);
        sprintf(info, "%sHeight:%d\n", info,m_format_ctx->streams[video_stream_index]->codecpar->height);
        if(m_format_ctx->streams[video_stream_index]->r_frame_rate.den!=0) {
            sprintf(info, "%sFrameRate:%f\n", info,
                    m_format_ctx->streams[video_stream_index]->r_frame_rate.num * 1.0 /
                    m_format_ctx->streams[video_stream_index]->r_frame_rate.den);
        }
        sprintf(info, "%sVideoBitRate:%lld\n", info,m_format_ctx->streams[video_stream_index]->codecpar->bit_rate);
        sprintf(info, "%sDuration:%lld\n", info,(int64_t)(m_format_ctx->streams[video_stream_index]->duration*av_q2d(m_format_ctx->streams[video_stream_index]->time_base)));
        //LOGE(TAG,"video time_base num %d,den %d ",m_format_ctx->streams[video_stream_index]->time_base.num,m_format_ctx->streams[video_stream_index]->time_base.den)
        //sprintf(info, "%sDuration2:%lld\n", info,m_format_ctx->duration/AV_TIME_BASE);
    }
    if(audio_stream_index!=-1){
//        sprintf(info, "%sMediaType:Audio\n", info);
        sprintf(info, "%sSampleRate:%d\n", info,m_format_ctx->streams[audio_stream_index]->codecpar->sample_rate);
        sprintf(info, "%sChannels:%d\n", info,m_format_ctx->streams[audio_stream_index]->codecpar->channels);
        sprintf(info, "%saudioBitRate:%lld\n", info,m_format_ctx->streams[audio_stream_index]->codecpar->bit_rate);
        //sprintf(info, "%sDuration:%f\n", info,m_format_ctx->streams[audio_stream_index]->duration*av_q2d(m_format_ctx->streams[audio_stream_index]->time_base));
        //LOGE(TAG," format: %d,  layout: %lld",m_format_ctx->streams[audio_stream_index]->codecpar->format,m_format_ctx->streams[audio_stream_index]->codecpar->channel_layout)
        //LOGE(TAG," time_base  %d",m_format_ctx->streams[audio_stream_index]->time_base.den)
    }
    return info;
}

int AudioAndVideoInfo::Init(const char *path) {
    this->m_path=path;
    //1，初始化上下文
    m_format_ctx = avformat_alloc_context();
    int state=0;
    //2，打开文件
    state=avformat_open_input(&m_format_ctx, m_path, NULL, NULL);
    if (state != 0) {
        LOG_ERROR(TAG, "avformat_open_input", "Fail to open file [%s]", m_path);
        return state;
    }

    //3，获取音视频流信息
    state=avformat_find_stream_info(m_format_ctx, NULL);
    if (state< 0) {
        LOG_ERROR(TAG, "avformat_find_stream_info", "Fail to find stream info");
        return state;
    }
    int vIdx = -1;//存放视频流的索引
    for (int i = 0; i < m_format_ctx->nb_streams; ++i) {
        if (m_format_ctx->streams[i]->codecpar->codec_type == video_type) {
            vIdx = i;
            break;
        }
    }
    if (vIdx == -1) {
        LOG_ERROR(TAG, "video", "Fail to find stream index")
    }
    else {
        video_stream_index = vIdx;
    }

    vIdx = -1;//存放视频流的索引
    for (int i = 0; i < m_format_ctx->nb_streams; ++i) {
        if (m_format_ctx->streams[i]->codecpar->codec_type == audio_type) {
            vIdx = i;
            break;
        }
    }
    if (vIdx == -1) {
        LOG_ERROR(TAG, "audio", "Fail to find stream index")
    }
    else {
        audio_stream_index = vIdx;
    }
    return 0;
}

AudioAndVideoInfo::~AudioAndVideoInfo() {
   if(m_format_ctx!=NULL) {
       avformat_close_input(&m_format_ctx);
       avformat_free_context(m_format_ctx);
       m_format_ctx=NULL;
   }
}
