//
// Created by deng on 2024/10/31.
//

#ifndef AUDIOANDVIDEOEDITOR_BASEENCODER_H
#define AUDIOANDVIDEOEDITOR_BASEENCODER_H

#include "../muxer/Mp4Muxer.h"

extern "C" {
#include <libavcodec/avcodec.h>
#include <libswscale/swscale.h>
#include <libavformat/avformat.h>
#include <libavutil/opt.h>
#include <libavutil/frame.h>
};
class BaseEncoder {
private:
    int64_t cur_time=0;
    const char * TAG = "BaseEncoder";
    // 编码格式 ID
    AVCodecID m_codec_id;
    // 编码器
    AVCodec *m_codec = NULL;

    // 编码上下文
    // 编码数据包
    AVPacket *m_encoded_pkt = NULL;

    // 写入Mp4的输入流索引
    int m_encode_stream_index = 0;
protected:
    Mp4Muxer *m_muxer = NULL;
    virtual void InitContext(AVCodecContext *codec_ctx) = 0;
    virtual void InitContext() = 0;
    virtual int ConfigureMuxerStream(Mp4Muxer *muxer, AVCodecContext *ctx) = 0;
//    virtual void Release() = 0;
    AVCodecContext *m_codec_ctx = NULL;

    /**
     * Log前缀
     */
    virtual const char *const LogSpec() = 0;
public:
    BaseEncoder(Mp4Muxer *muxer, AVCodecID codec_id);
    ~BaseEncoder();
    int OpenEncoder(AVCodecContext *codec_ctx);
    int OpenEncoder();
    AVRational m_src_time_base;
    int EncodeOneFrame(AVFrame * frame);
    int Init();
    int64_t getCurTime(){
        return cur_time;
    }
    AVCodecContext * getAVCodecContext(){
        return m_codec_ctx;
    }
};


#endif //AUDIOANDVIDEOEDITOR_BASEENCODER_H
