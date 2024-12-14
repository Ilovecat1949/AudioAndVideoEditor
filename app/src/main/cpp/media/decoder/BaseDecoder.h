//
// Created by deng on 2024/10/31.
//

#ifndef AUDIOANDVIDEOEDITOR_BASEDECODER_H
#define AUDIOANDVIDEOEDITOR_BASEDECODER_H


#include <stddef.h>
#include "../../utils/one_frame.h"

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/frame.h>
#include <libavutil/time.h>
};
class BaseDecoder {
private:
    const char *TAG = "BaseDecoder";
    // 经过转换的路径
    const char *m_path = NULL;
    /**
     * 初始化FFMpeg相关的参数
     * @param env jvm环境
     */
    int InitFFMpegDecoder();
    /**
     * 分配解码过程中需要的缓存
     */
    void AllocFrameBuffer();
    /**
     * 获取当前帧时间戳
     */
    void ObtainTimeStamp();
public:
    BaseDecoder(const char * path);
    virtual ~BaseDecoder();
    int Init();
    virtual OneFrame *getOneFrame()=0;
    /**
  * 获取解码时间基
  */
virtual AVRational time_base() {
        return m_format_ctx->streams[m_stream_index]->time_base;
    }
    /**
     * 视频宽度
     * @return
     */
    int width() {
        return m_codec_ctx->width;
    }
    AVCodecContext * getAVCodecContext(){
        return m_codec_ctx;
    }

    /**
     * 视频高度
     * @return
     */
    int height() {
        return m_codec_ctx->height;
    }
    int64_t GetDuration() ;
    int64_t GetCurPos() ;
    int DecodeOneFrame();
    void DoneDecode();
protected:
    // 解码信息上下文
    AVFormatContext *m_format_ctx = NULL;
    // 解码器
    AVCodec *m_codec = NULL;
    // 解码器上下文
    AVCodecContext *m_codec_ctx = NULL;
    // 待解码包
    AVPacket *m_packet = NULL;
    // 最终解码数据
    AVFrame *m_frame = NULL;
    // 当前播放时间
    int64_t m_cur_t_s = 0;
    // 总时长
    int64_t m_duration = 0;
    // 开始播放的时间
    int64_t m_started_t = -1;
    // 数据流索引
    int m_stream_index = -1;
    /**
     * 是否为合成器提供解码
     * @return true 为合成器提供解码 false 解码播放
     */
    const char * path() {
        return m_path;
    }
    /**
     * 解码器上下文
     * @return
     */
    AVCodecContext *codec_cxt() {
        return m_codec_ctx;
    }
    /**
     * 视频数据编码格式
     * @return
     */
    AVPixelFormat video_pixel_format() {
        return m_codec_ctx->pix_fmt;
    }

    /**
     * 解码一帧数据
     * @return
     */
    /**
     * 音视频索引
     */
    virtual AVMediaType GetMediaType() = 0;
    virtual AVFrame * getAVFrame()=0;
    virtual const char *const LogSpec() = 0;
};


#endif //AUDIOANDVIDEOEDITOR_BASEDECODER_H
