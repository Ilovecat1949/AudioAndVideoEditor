//
// Created by deng on 2024/10/31.
//

#include "BaseEncoder.h"
#include "../../utils/logger.h"

BaseEncoder::BaseEncoder(Mp4Muxer *muxer, AVCodecID codec_id)
        : m_muxer(muxer),
          m_codec_id(codec_id) {
}

int BaseEncoder::Init() {
//    m_codec = avcodec_find_encoder_by_name("mpeg4");
    m_codec = avcodec_find_encoder(m_codec_id);
    //LOGE(TAG, "encoder name is %s", m_codec->name)
    if (m_codec == NULL) {
        LOGE(TAG, "Fail to find encoder, code id is %d", m_codec_id)
        return -1;
    }
    m_codec_ctx = avcodec_alloc_context3(m_codec);
    if (m_codec_ctx == NULL) {
        LOGE(TAG, "Fail to alloc encoder context")
        return -1;
    }
    m_encoded_pkt = av_packet_alloc();
    av_init_packet(m_encoded_pkt);
    return 0;
}

int BaseEncoder::OpenEncoder(AVCodecContext *codec_ctx) {
    InitContext(codec_ctx);
    int ret = avcodec_open2(m_codec_ctx, m_codec, NULL);
    LOGI(LogSpec(),"sample_rate %d  frame:%d profiles %d",m_codec_ctx->sample_rate,m_codec_ctx->frame_size,m_codec_ctx->profile);
    if (ret < 0) {
        LOG_ERROR(TAG, LogSpec(), "Fail to open encoder : %d", ret);//m_codec
        return ret;
    }
    m_encode_stream_index = ConfigureMuxerStream(m_muxer, m_codec_ctx);
    return 0;
}
int BaseEncoder::OpenEncoder() {
    InitContext();
    int ret = avcodec_open2(m_codec_ctx, m_codec, NULL);
    //LOGI(LogSpec(),"sample_rate %d  frame:%d profiles %d",m_codec_ctx->sample_rate,m_codec_ctx->frame_size,m_codec_ctx->profile);
    if (ret < 0) {
        LOG_ERROR(TAG, LogSpec(), "Fail to open encoder : %d", ret);//m_codec
        return ret;
    }
    m_encode_stream_index = ConfigureMuxerStream(m_muxer, m_codec_ctx);
    return 0;
}
int BaseEncoder::EncodeOneFrame(AVFrame * frame) {
    //LOGI(TAG, "codec_id %u nb_samples %d frame_size %d",m_codec_ctx->codec_id,frame->nb_samples,m_codec_ctx->frame_size)
    bool flag= false;
    int state;

    while(1) {
        do{
            state= avcodec_receive_packet(m_codec_ctx, m_encoded_pkt);
            switch (state) {
                case 0:
                    av_packet_rescale_ts(m_encoded_pkt, m_src_time_base,
                                         m_muxer->GetTimeBase(m_encode_stream_index));
                    this->cur_time = (int64_t) (m_encoded_pkt->pts *
                                             av_q2d(m_muxer->GetTimeBase(m_encode_stream_index)) *
                                             1000);
//                    LOGE(TAG,"%s m_src_time_base den:%d , num  %d",LogSpec(),m_src_time_base.den,m_src_time_base.num)
//                    LOGE(TAG,"%s m_encoded_pkt->pts %lld",LogSpec(),m_encoded_pkt->pts)
//                    LOGE(TAG,"%s m_muxer->GetTimeBase den:%d , num  %d",LogSpec(),m_muxer->GetTimeBase(m_encode_stream_index).den,m_muxer->GetTimeBase(m_encode_stream_index).num)
//                    LOGE(TAG,"%s cur_time %lld",LogSpec(),this->cur_time)
                    m_encoded_pkt->stream_index = m_encode_stream_index;
                    m_muxer->Write(m_encoded_pkt);
//            free(m_encoded_pkt);
                    LOGE(TAG,"%s receive frame to encode ok",LogSpec())
                    break;
                case AVERROR_EOF: //解码结束
                    LOG_ERROR(TAG, LogSpec(), "Encode finish")
                    return AVERROR_EOF;
                case AVERROR(EAGAIN): //编码还未完成，待会再来
                    LOG_INFO(TAG, LogSpec(), "Encode error[EAGAIN]: %s", av_err2str(AVERROR(EAGAIN)));
//                return AVERROR(EAGAIN);
                    break;
                case AVERROR(EINVAL):
                    LOG_ERROR(TAG, LogSpec(), "Encode error[EINVAL]: %s", av_err2str(AVERROR(EINVAL)));
//                return AVERROR(EINVAL);
                    break;
                case AVERROR(ENOMEM):
                    LOG_ERROR(TAG, LogSpec(), "Encode error[ENOMEM]: %s", av_err2str(AVERROR(ENOMEM)));
//                return AVERROR(ENOMEM);
                    break;
                default:
                    LOG_ERROR(TAG, LogSpec(), "Encode error[ENOMEM]: %s", av_err2str(state));
                    break;
            }
            av_packet_unref(m_encoded_pkt);
        }while(state==0);
        if(flag){
            //break;
            return 0;
        }
        else {
            state= avcodec_send_frame(m_codec_ctx, frame);
            switch (state) {
                case 0:
                    flag= true;
                    LOGE(TAG, "%s Send frame to encode ok",LogSpec())
                    break;
                case AVERROR_EOF:
                    LOG_INFO(TAG, LogSpec(), "Send frame finish [AVERROR_EOF]")
                    flag= true;
                    break;
                case AVERROR(EAGAIN): //编码编码器已满，先取出已编码数据，再尝试发送数据
                    break;
                case AVERROR(EINVAL):
                    LOG_ERROR(TAG, LogSpec(), "Send frame error[EINVAL]: %s,%d",
                              av_err2str(AVERROR(EINVAL)), AVERROR(EINVAL));
                    return AVERROR(EINVAL);
                case AVERROR(ENOMEM):
                    LOG_ERROR(TAG, LogSpec(), "Send frame error[ENOMEM]: %s",
                              av_err2str(AVERROR(ENOMEM)));
                    return AVERROR(ENOMEM);
                default:
                    LOG_ERROR(TAG, LogSpec(), "Send frame other errors : %s",
                              av_err2str(state));
                    break;
            }
        }
    }
}
BaseEncoder::~BaseEncoder() {
    if (m_encoded_pkt != NULL) {
        av_packet_free(&m_encoded_pkt);
        //m_encoded_pkt = NULL;
    }
    if (m_codec_ctx != NULL) {
        avcodec_close(m_codec_ctx);
        avcodec_free_context(&m_codec_ctx);
    }
}


