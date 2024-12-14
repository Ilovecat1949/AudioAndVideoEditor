//
// Created by deng on 2024/10/31.
//

#ifndef AUDIOANDVIDEOEDITOR_AUDIOENCODER_H
#define AUDIOANDVIDEOEDITOR_AUDIOENCODER_H

#include "../BaseEncoder.h"

extern "C" {
#include <libswresample/swresample.h>
};
class AudioEncoder: public BaseEncoder  {

private:
    //AVFrame *m_frame = NULL;
    // 音频转换器
    SwrContext *m_swr = NULL;
    void InitFrame();
    AVSampleFormat m_sample_fmt;
    int m_sample_rate=-1;
    uint64_t m_channel_layout;
    int m_channels=-1;
    int64_t m_bit_rate=-1;
    int m_profile=-1;

protected:
    void InitContext(AVCodecContext *codec_ctx) override;
    void InitContext() override;
    int ConfigureMuxerStream(Mp4Muxer *muxer, AVCodecContext *ctx) override;


    const char *const LogSpec() override {
        return "音频";
    };
//    void Release() override;
public:
    AudioEncoder(Mp4Muxer *muxer);
    ~AudioEncoder();
    /**
 * 初始化转换工具
 */
    void InitSwr();
    AVFrame * ReSampling(AVFrame * src_frame);
    void setTargetPara(
            AVSampleFormat sample_fmt,
            int sample_rate,
            uint64_t channel_layout,
            int channels,
            int64_t bit_rate,
            int profile
            ){
        m_sample_fmt=sample_fmt;
        m_sample_rate=sample_rate;
        m_channel_layout=channel_layout;
        m_channels=channels;
        m_bit_rate=bit_rate;
        m_profile =profile ;
    }
};


#endif //AUDIOANDVIDEOEDITOR_AUDIOENCODER_H
