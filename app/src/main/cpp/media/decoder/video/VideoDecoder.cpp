//
// Created by deng on 2024/10/31.
//

#include "VideoDecoder.h"
#include "../../../utils/logger.h"

VideoDecoder::VideoDecoder(const char *path): BaseDecoder(path){
}

VideoDecoder::~VideoDecoder() {
    LOGE(TAG, "[VIDEO] release")
    if (m_rgb_frame != NULL) {
        av_frame_free(&m_rgb_frame);
        m_rgb_frame = NULL;
    }
    if (m_buf_for_rgb_frame != NULL) {
        free(m_buf_for_rgb_frame);
        m_buf_for_rgb_frame = NULL;
    }
    if (m_sws_ctx != NULL) {
        sws_freeContext(m_sws_ctx);
        m_sws_ctx = NULL;
    }
    LOG_INFO(TAG, LogSpec(), "Decode done and decoder release")
}

AVFrame *  VideoDecoder::getAVFrame() {
    if (m_frame != NULL){
        AVFrame *frame = av_frame_alloc();
        if(frame==NULL){
            return NULL;
        }
        int numBytes = av_image_get_buffer_size(m_codec_ctx->pix_fmt, m_codec_ctx->width,
                                                m_codec_ctx->height, 1);
        uint8_t *m_buf_for_frame = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));
        if(m_buf_for_frame==NULL){
            av_frame_free(&frame);
            av_freep(&m_buf_for_frame);
            return NULL;
        }
        int state=av_image_fill_arrays(frame->data, frame->linesize,
                             m_buf_for_frame, m_codec_ctx->pix_fmt, m_codec_ctx->width,
                             m_codec_ctx->height, 1);
        if(state<0){
            av_frame_free(&frame);
            av_freep(&m_buf_for_frame);
            return NULL;
        }
        frame->format=m_frame->format;
        frame->height=m_frame->height;
        frame->width=m_frame->width;
        frame->pts=m_frame->pts;
        av_frame_copy(frame,m_frame);
        av_frame_copy_props(frame,m_frame);
        return frame;
    }
    else{
        LOGI(TAG, "getAVFrame is NULL")
        return NULL;
    }
}

OneFrame *VideoDecoder::getOneFrame() {
    if (m_frame != NULL){
        AVFrame *frame = av_frame_alloc();
        if(frame==NULL){
            return NULL;
        }
        int numBytes = av_image_get_buffer_size(m_codec_ctx->pix_fmt, m_codec_ctx->width,
                                                m_codec_ctx->height, 1);
        uint8_t *m_buf_for_frame = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));
        if(m_buf_for_frame==NULL){
            av_frame_free(&frame);
            av_freep(&m_buf_for_frame);
            return NULL;
        }
        int state=av_image_fill_arrays(frame->data, frame->linesize,
                                       m_buf_for_frame, m_codec_ctx->pix_fmt, m_codec_ctx->width,
                                       m_codec_ctx->height, 1);
        if(state<0){
            av_frame_free(&frame);
            av_freep(&m_buf_for_frame);
            return NULL;
        }
        frame->format=m_frame->format;
        frame->height=m_frame->height;
        frame->width=m_frame->width;
        frame->pts=m_frame->pts;
        av_frame_copy(frame,m_frame);
        av_frame_copy_props(frame,m_frame);
        return new OneFrame(frame,m_buf_for_frame, time_base());
    }
    else{
        LOGI(TAG, "getAVFrame is NULL")
        return NULL;
    }
}
