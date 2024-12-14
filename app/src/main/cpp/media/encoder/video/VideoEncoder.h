//
// Created by deng on 2024/10/31.
//

#ifndef AUDIOANDVIDEOEDITOR_VIDEOENCODER_H
#define AUDIOANDVIDEOEDITOR_VIDEOENCODER_H


#include "../BaseEncoder.h"

class VideoEncoder : public BaseEncoder {
private:

    const char * TAG = "VideoEncoder";

    SwsContext *m_sws_ctx = NULL;

    AVFrame *m_yuv_frame = NULL;

    int m_width = -1;
    int m_height = -1;
    AVRational m_framerate={-1,-1};
    AVPixelFormat m_pix_fmt;
    int64_t m_bit_rate=-1;
    int thread_count=8;
    void InitYUVFrame();

protected:

    const char *const LogSpec() override {
        return "视频";
    };

    void InitContext(AVCodecContext *codec_ctx) override;
    void InitContext() override;
    int ConfigureMuxerStream(Mp4Muxer *muxer, AVCodecContext *ctx) override;
//    AVFrame* DealFrame(OneFrame *one_frame) override;

//    void Release() override;
public:
    void setTargetPara(
            int width,
            int height,
            AVRational framerate,
            AVPixelFormat pix_fmt,
            int64_t bit_rate
            ){
    m_width=width;
    m_height=height;
    m_framerate=framerate;
    m_pix_fmt=pix_fmt;
    m_bit_rate=bit_rate;
    };
    VideoEncoder(Mp4Muxer *muxer);

    ~VideoEncoder();
};


#endif //AUDIOANDVIDEOEDITOR_VIDEOENCODER_H
