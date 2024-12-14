//
// Created by deng on 2024/10/31.
//

#include "AudioEncoder.h"
#include "../../../utils/logger.h"

AudioEncoder::AudioEncoder(Mp4Muxer *muxer)
        : BaseEncoder(muxer, AV_CODEC_ID_AAC) {
}
void AudioEncoder::InitContext(AVCodecContext *codec_ctx) {
    this->m_codec_ctx->codec_id=AV_CODEC_ID_AAC;
    this->m_codec_ctx->codec_type=AVMEDIA_TYPE_AUDIO;
    this->m_codec_ctx->sample_fmt=codec_ctx->sample_fmt ;
    this->m_codec_ctx->sample_rate=codec_ctx->sample_rate;
    this->m_codec_ctx->channel_layout=codec_ctx->channel_layout;
    this->m_codec_ctx->channels=codec_ctx->channels;
    this->m_codec_ctx->bit_rate=codec_ctx->bit_rate;//64000
    this->m_codec_ctx->profile=codec_ctx->profile;
//    this->m_codec_ctx->frame_size=codec_ctx->frame_size;
//    this->m_codec_ctx->codec_id = AV_CODEC_ID_AAC;
//    this->m_codec_ctx->codec_type = AVMEDIA_TYPE_AUDIO;
//    this->m_codec_ctx->sample_fmt = AV_SAMPLE_FMT_FLTP;
//    this->m_codec_ctx->sample_rate = 44100;
//    this->m_codec_ctx->channel_layout = AV_CH_LAYOUT_STEREO;
//    this->m_codec_ctx->channels = 2;
//    this->m_codec_ctx->bit_rate = 64000;
    InitFrame();
}
void AudioEncoder::InitContext() {
    this->m_codec_ctx->codec_id=AV_CODEC_ID_AAC;
    this->m_codec_ctx->codec_type=AVMEDIA_TYPE_AUDIO;
    this->m_codec_ctx->sample_fmt=m_sample_fmt ;
    this->m_codec_ctx->sample_rate=m_sample_rate;
    this->m_codec_ctx->channel_layout=m_channel_layout;
    this->m_codec_ctx->channels=m_channels;
    this->m_codec_ctx->bit_rate=m_bit_rate;//64000
    this->m_codec_ctx->profile=m_profile;
}
void AudioEncoder::InitFrame() {
//    m_frame = av_frame_alloc();
//    m_frame->nb_samples = 1024;
//    m_frame->format = ENCODE_AUDIO_DEST_FORMAT;
//    m_frame->channel_layout = ENCODE_AUDIO_DEST_CHANNEL_LAYOUT;
//
//    int size = av_samples_get_buffer_size(NULL, ENCODE_AUDIO_DEST_CHANNEL_COUNTS, m_frame->nb_samples,
//                                          ENCODE_AUDIO_DEST_FORMAT, 1);
//    uint8_t *frame_buf = (uint8_t *) av_malloc(size);
//    avcodec_fill_audio_frame(m_frame, ENCODE_AUDIO_DEST_CHANNEL_COUNTS, ENCODE_AUDIO_DEST_FORMAT,
//                             frame_buf, size, 1);
//
//    LOGE("AudioEncoder", "InitFrame size : %d", size)
//    LOGE("AudioEncoder", "frame data size : %d", malloc_usable_size(m_frame->data))
//    LOGE("AudioEncoder", "frame data size1: %d", malloc_usable_size(m_frame->data[0]))
//    LOGE("AudioEncoder", "frame data size2: %d", malloc_usable_size(m_frame->data[1]))
}
int AudioEncoder::ConfigureMuxerStream(Mp4Muxer *muxer, AVCodecContext *ctx) {
    return muxer->AddAudioStream(ctx);
}

//void AudioEncoder::Release() {
//
//}
AVFrame *AudioEncoder::ReSampling(AVFrame *src_frame) {
    if(src_frame!=NULL) {
        //m_codec_ctx->frame_size=2048;
        AVFrame *frame = av_frame_alloc();
        frame->nb_samples = m_codec_ctx->frame_size;
        LOGI(LogSpec(), "codec_id %u frame_size %d",m_codec_ctx->codec_id,m_codec_ctx->frame_size)
        frame->format = m_codec_ctx->sample_fmt;
        frame->channel_layout = m_codec_ctx->channel_layout;
        int size = av_samples_get_buffer_size(NULL, m_codec_ctx->channels, 2048,
                                              m_codec_ctx->sample_fmt, 1);
        uint8_t *frame_buf = (uint8_t *) av_malloc(size);
        avcodec_fill_audio_frame(frame, m_codec_ctx->channels, m_codec_ctx->sample_fmt,
                                 frame_buf, size, 1);
        frame->channels = m_codec_ctx->channels;
        frame->pts=src_frame->pts;
        // 重采样后一个通道采样数
        int m_dest_nb_sample = (int)av_rescale_rnd(frame->nb_samples, m_codec_ctx->sample_rate,
                                                   m_codec_ctx->sample_rate, AV_ROUND_UP);
        // 重采样后一帧数据的大小
        size_t m_dest_data_size = (size_t)av_samples_get_buffer_size(
                NULL, m_codec_ctx->channels,
                m_dest_nb_sample, m_codec_ctx->sample_fmt, 1);
        LOGI(LogSpec(),"m_dest_data_size %d ",m_dest_data_size)
//        int ret=1;
        int ret = swr_convert(m_swr, frame->data, 2048,
                              (const uint8_t **) src_frame->data, src_frame->nb_samples);
//        memcpy(frame->data[0], src_frame->data[0], 4096);
//        memcpy(frame->data[1], src_frame->data[1], 4096);
        av_frame_copy_props(frame, src_frame);
        LOGI(LogSpec(),"ReSampling %d ",ret)
        LOGI(LogSpec(), "frame_size %d",frame->nb_samples)
        if (ret > 0){
            return frame;
        } else{
            LOGI(LogSpec(),"ReSampling %d ",ret)
        }
    }
    return NULL;
}
void AudioEncoder::InitSwr() {
    //初始化格式转换工具
    m_swr = swr_alloc();

    av_opt_set_int(m_swr, "in_channel_layout", m_codec_ctx ->channel_layout, 0);
    av_opt_set_int(m_swr, "out_channel_layout", m_codec_ctx ->channel_layout, 0);

    av_opt_set_int(m_swr, "in_sample_rate", m_codec_ctx ->sample_rate, 0);
    av_opt_set_int(m_swr, "out_sample_rate", m_codec_ctx ->sample_rate, 0);

    av_opt_set_sample_fmt(m_swr, "in_sample_fmt", m_codec_ctx->sample_fmt, 0);
    av_opt_set_sample_fmt(m_swr, "out_sample_fmt", m_codec_ctx->sample_fmt,  0);

    swr_init(m_swr);
}
AudioEncoder::~AudioEncoder() {
    m_muxer->EndAudioStream();
}


