//
// Created by deng on 2024/10/31.
//

#include "AudioDecoder.h"
#include "../../../utils/logger.h"

AudioDecoder::AudioDecoder(const char * path) : BaseDecoder(path) {
}

AudioDecoder::~AudioDecoder() {
    LOG_INFO(TAG, LogSpec(), "Decode done and decoder release")
    if (m_swr != NULL) {
        swr_free(&m_swr);
    }
}

AVFrame * AudioDecoder::getAVFrame() {
    if(m_frame!=NULL){
        AVFrame *frame = av_frame_alloc();
        if(frame==NULL){
            return NULL;
        }
        frame->nb_samples =m_frame->nb_samples;
        LOGI(TAG, "codec_id %u,profile %d,sample_rate %d,bit_rate %lld,nb_samples %d frame_size %d",m_codec_ctx->codec_id,m_codec_ctx->profile,m_codec_ctx->sample_rate,m_codec_ctx->bit_rate,m_frame->nb_samples,m_codec_ctx->frame_size)
        frame->format = m_codec_ctx->sample_fmt;
        frame->channel_layout = m_codec_ctx->channel_layout;
        int size = av_samples_get_buffer_size(NULL, m_codec_ctx->channels, m_frame->nb_samples,
                                              m_codec_ctx->sample_fmt, 1);
        uint8_t *m_buf_for_frame = (uint8_t *) av_malloc(size);
        if(m_buf_for_frame == NULL){
            av_frame_free(&frame);
            av_freep(&m_buf_for_frame);
            return NULL;
        }
        int state=avcodec_fill_audio_frame(frame, m_codec_ctx->channels, m_codec_ctx->sample_fmt,
                                           m_buf_for_frame, size, 1);
        if(state<0){
            av_frame_free(&frame);
            av_freep(&m_buf_for_frame);
            return NULL;
        }
        frame->channels=m_frame->channels;
        //frame->pts=m_frame->pts;
        frame->pts=m_pts;
        m_pts+=1;
        av_frame_copy(frame,m_frame);
        av_frame_copy_props(frame,m_frame);
        return frame;
    }
    else{
        LOGI(TAG, "getAVFrame is NULL")
        return NULL;
    }
}
void AudioDecoder::InitSwr() {

}

void AudioDecoder::CalculateSampleArgs() {

}

OneFrame *AudioDecoder::getOneFrame() {
    if(m_frame!=NULL){
        AVFrame *frame = av_frame_alloc();
        if(frame==NULL){
            return NULL;
        }
        frame->nb_samples =m_frame->nb_samples;
        LOGI(TAG, "codec_id %u,profile %d,sample_rate %d,bit_rate %lld,nb_samples %d frame_size %d",m_codec_ctx->codec_id,m_codec_ctx->profile,m_codec_ctx->sample_rate,m_codec_ctx->bit_rate,m_frame->nb_samples,m_codec_ctx->frame_size)
        frame->format = m_codec_ctx->sample_fmt;
        frame->channel_layout = m_codec_ctx->channel_layout;
        int size = av_samples_get_buffer_size(NULL, m_codec_ctx->channels, m_frame->nb_samples,
                                              m_codec_ctx->sample_fmt, 1);
        uint8_t *m_buf_for_frame = (uint8_t *) av_malloc(size);
        if(m_buf_for_frame == NULL){
            av_frame_free(&frame);
            av_freep(&m_buf_for_frame);
            return NULL;
        }
        int state=avcodec_fill_audio_frame(frame, m_codec_ctx->channels, m_codec_ctx->sample_fmt,
                                           m_buf_for_frame, size, 1);
        if(state<0){
            av_frame_free(&frame);
            av_freep(&m_buf_for_frame);
            return NULL;
        }
        frame->channels=m_frame->channels;
        //frame->pts=m_frame->pts;
        frame->pts=m_pts;
        m_pts+=1;
        av_frame_copy(frame,m_frame);
        av_frame_copy_props(frame,m_frame);
        return new OneFrame(frame,m_buf_for_frame, time_base());
    }
    else{
        LOGI(TAG, "getAVFrame is NULL")
        return NULL;
    }
}
