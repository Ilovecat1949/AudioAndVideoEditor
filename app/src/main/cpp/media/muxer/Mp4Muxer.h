//
// Created by deng on 2024/10/31.
//

#ifndef AUDIOANDVIDEOEDITOR_MP4MUXER_H
#define AUDIOANDVIDEOEDITOR_MP4MUXER_H

#include "../../utils/logger.h"
#include <mutex>
extern "C" {
#include <libavformat/avformat.h>
};
class Mp4Muxer {
private:
    std::mutex mtx;
    const char *TAG = "Mp4Muxer";

    const char *m_path;

    AVFormatContext * m_fmt_ctx = NULL;

    bool m_audio_configured = false;

    bool m_audio_end = false;

    bool m_video_configured = false;

    bool m_video_end = false;


    int AddStream(AVCodecContext *ctx);

public:
    Mp4Muxer();

    ~Mp4Muxer();

    AVRational GetTimeBase(int stream_index) {
//        LOGI(TAG,"sample_rate %d",m_fmt_ctx->streams[stream_index]->codecpar->sample_rate);
//        LOGI(TAG,"time_base.den %d",m_fmt_ctx->streams[stream_index]->time_base.den);
        return m_fmt_ctx->streams[stream_index]->time_base;
    }

    int Init(const char * path);

    int AddVideoStream(AVCodecContext *ctx);
    int AddAudioStream(AVCodecContext *ctx);

    int Start();

    int Write(AVPacket *pkt);

    void EndVideoStream();

    void EndAudioStream();

    void Release();
};


#endif //AUDIOANDVIDEOEDITOR_MP4MUXER_H
