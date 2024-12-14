//
// Created by deng on 2024/10/31.
//

#ifndef AUDIOANDVIDEOEDITOR_AUDIODECODER_H
#define AUDIOANDVIDEOEDITOR_AUDIODECODER_H

#include "../BaseDecoder.h"
#include "../../../utils/logger.h"

extern "C" {
#include <libswresample/swresample.h>
#include <libavutil/opt.h>
#include <libavutil/audio_fifo.h>
};
class AudioDecoder : public BaseDecoder {
private:
    int64_t m_pts = 0;
    const char *TAG = "AudioDecoder";
    // 音频转换器
    SwrContext *m_swr = NULL;
    /**
     * 初始化转换工具
     */
    void InitSwr();
    /**
     * 计算重采样后通道采样数和帧数据大小
     */
    void CalculateSampleArgs();
public:
    AudioDecoder(const char * path);
    ~AudioDecoder();
    AVFrame * getAVFrame() override;
    int getSampleRate(){
        return  m_codec_ctx->sample_rate;
    }
    int getChannels(){
        return m_codec_ctx->channels;
    }
    int64_t getBitRate(){
        return m_codec_ctx->bit_rate;
    }
    AVSampleFormat getSampleFmt(){
        return m_codec_ctx->sample_fmt;
    }
    uint64_t getChannelLayout(){
        return m_codec_ctx->channel_layout;
    }
    int getProfile(){
        return m_codec_ctx->profile ;
    }
    AVRational time_base() override{
        return {1,this->m_codec_ctx->sample_rate};
    }
    int getSampleBufferSize(){
        //LOGI(TAG, "getSampleBufferSize   codec_id %u,profile %d,sample_rate %d,bit_rate %lld,nb_samples %d frame_size %d",m_codec_ctx->codec_id,m_codec_ctx->profile,m_codec_ctx->sample_rate,m_codec_ctx->bit_rate,m_frame->nb_samples,m_codec_ctx->frame_size)
        return av_samples_get_buffer_size(NULL, m_codec_ctx->channels, m_codec_ctx->frame_size,
                                          m_codec_ctx->sample_fmt, 1);
    }
    OneFrame *getOneFrame() override;
protected:
    AVMediaType GetMediaType() override {
        return AVMEDIA_TYPE_AUDIO;
    }
    const char *const LogSpec() override {
        return "AUDIO";
    };
};


#endif //AUDIOANDVIDEOEDITOR_AUDIODECODER_H
