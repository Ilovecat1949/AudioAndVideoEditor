//
// Created by deng on 2024/10/31.
//

#ifndef AUDIOANDVIDEOEDITOR_VIDEODECODER_H
#define AUDIOANDVIDEOEDITOR_VIDEODECODER_H

#include "../BaseDecoder.h"

extern "C" {
#include <libavutil/imgutils.h>
#include <libswscale/swscale.h>
};
class VideoDecoder : public BaseDecoder  {
private:
    const char *TAG = "VideoDecoder";
    //视频数据目标格式
    const AVPixelFormat DST_FORMAT = AV_PIX_FMT_RGBA;

    //存放YUV转换为RGB后的数据
    AVFrame *m_rgb_frame = NULL;

    uint8_t *m_buf_for_rgb_frame = NULL;

    //视频格式转换器
    SwsContext *m_sws_ctx = NULL;
protected:
    AVMediaType GetMediaType() override {
        return AVMEDIA_TYPE_VIDEO;
    }
    const char *const LogSpec() override {
        return "VIDEO";
    };
public:
    VideoDecoder(const char* path);
    AVFrame * getAVFrame() override;
    int getWidth(){
        return m_codec_ctx->width;
    }
    int getHeight(){
        return m_codec_ctx->height;
    }
    AVRational getFrameRate(){
        return m_format_ctx->streams[m_stream_index]->r_frame_rate;
    }
    int64_t  getBitRate(){
        return m_codec_ctx->bit_rate;
    }
    AVPixelFormat getPixelFormat(){
        return m_codec_ctx->pix_fmt;
    }
    int getImageBufferSize(){
        return av_image_get_buffer_size(m_codec_ctx->pix_fmt, m_codec_ctx->width,
                                        m_codec_ctx->height, 1);
    }
    ~VideoDecoder();
    OneFrame *getOneFrame() override;

};


#endif //AUDIOANDVIDEOEDITOR_VIDEODECODER_H
