//
// Created by deng on 2024/10/31.
//

#include "BaseDecoder.h"
#include "../../utils/logger.h"

BaseDecoder::BaseDecoder(const char *path) {
    this->m_path=path;
}

int BaseDecoder::Init(){
    int state=0;
    state=InitFFMpegDecoder();
    if(state<0){
        return state;
    }
    AllocFrameBuffer();
    return 0;
}
BaseDecoder::~BaseDecoder() {
    DoneDecode();
    if (m_format_ctx != NULL) delete m_format_ctx;
    if (m_codec_ctx != NULL) delete m_codec_ctx;
    if (m_frame != NULL) delete m_frame;
    if (m_packet != NULL) delete m_packet;
}
void BaseDecoder::DoneDecode() {
//    LOG_INFO(TAG, LogSpec(), "Decode done and decoder release")
    // 释放缓存
    if (m_packet != NULL) {
        av_packet_free(&m_packet);
        m_packet=NULL;
    }
    if (m_frame != NULL) {
        av_frame_free(&m_frame);
        m_frame=NULL;
    }
    // 关闭解码器
    if (m_codec_ctx != NULL) {
        avcodec_close(m_codec_ctx);
        avcodec_free_context(&m_codec_ctx);
        m_codec_ctx=NULL;
    }
    // 关闭输入流
    if (m_format_ctx != NULL) {
        avformat_close_input(&m_format_ctx);
        avformat_free_context(m_format_ctx);
        m_format_ctx=NULL;
    }
    // 释放转换参数
    // 通知子类释放资源
//    Release();
}
int BaseDecoder::InitFFMpegDecoder() {
    //1，初始化上下文
    m_format_ctx = avformat_alloc_context();
    int state=0;
    //2，打开文件
    state=avformat_open_input(&m_format_ctx, m_path, NULL, NULL);
    if (state != 0) {
        LOG_ERROR(TAG, LogSpec(), "Fail to open file [%s]", m_path);
        DoneDecode();
        return state;
    }

    //3，获取音视频流信息
    state=avformat_find_stream_info(m_format_ctx, NULL);
    if (state< 0) {
        LOG_ERROR(TAG, LogSpec(), "Fail to find stream info");
        DoneDecode();
        return state;
    }
    //4，查找编解码器
    //4.1 获取视频流的索引
    int vIdx = -1;//存放视频流的索引
    for (int i = 0; i < m_format_ctx->nb_streams; ++i) {
        if (m_format_ctx->streams[i]->codecpar->codec_type == GetMediaType()) {
            vIdx = i;
            break;
        }
    }
    if (vIdx == -1) {
        LOG_ERROR(TAG, LogSpec(), "Fail to find stream index")
        DoneDecode();
        return -1;
    }
    m_stream_index = vIdx;

    //4.2 获取解码器参数
    AVCodecParameters *codecPar = m_format_ctx->streams[vIdx]->codecpar;

    //4.3 获取解码器
//    m_codec = avcodec_find_decoder_by_name("h264_mediacodec");//硬解码
    m_codec = avcodec_find_decoder(codecPar->codec_id);
    LOG_INFO(TAG, LogSpec(), "codecPar->codec_id %u",codecPar->codec_id);
    //4.4 获取解码器上下文
    m_codec_ctx = avcodec_alloc_context3(m_codec);
    state=avcodec_parameters_to_context(m_codec_ctx, codecPar);
    if (state != 0) {
        LOG_ERROR(TAG, LogSpec(), "Fail to obtain av codec context");
        DoneDecode();
        return state;
    }
    //5，打开解码器
    state=avcodec_open2(m_codec_ctx, m_codec, NULL);
    if (state< 0) {
        LOG_ERROR(TAG, LogSpec(), "Fail to open av codec");
        DoneDecode();
        return state;
    }
    m_duration = (int64_t)((float)m_format_ctx->duration/AV_TIME_BASE * 1000);
    LOG_INFO(TAG, LogSpec(), "m_duration %lld",m_duration)
    LOG_INFO(TAG, LogSpec(), "frame %d",m_codec_ctx->frame_size)
    LOG_INFO(TAG, LogSpec(), "profile %d",m_codec_ctx->profile)
    LOG_INFO(TAG, LogSpec(), "bit_rate %lld",m_codec_ctx->bit_rate)
    LOG_INFO(TAG, LogSpec(), "height %d",m_codec_ctx->height)
    LOG_INFO(TAG, LogSpec(), "width %d",m_codec_ctx->width)
//    av_parser_parse2()
    LOG_INFO(TAG, LogSpec(), "Decoder init success");
    return 0;
}
void BaseDecoder::AllocFrameBuffer() {
    // 初始化待解码和解码数据结构
    // 1）初始化AVPacket，存放解码前的数据
    m_packet = av_packet_alloc();
    // 2）初始化AVFrame，存放解码后的数据
    m_frame = av_frame_alloc();
}
int BaseDecoder::DecodeOneFrame() {
    bool flag;
    while(1){
        int result = avcodec_receive_frame(m_codec_ctx, m_frame);
        if (result == 0) {
            ObtainTimeStamp();
            LOG_INFO(TAG, LogSpec(), "m_cur_t_s %lld",m_cur_t_s);
            return result;
        } else {
            if(m_packet!=NULL){
                av_packet_unref(m_packet);
            }
            LOG_INFO(TAG, LogSpec(), "Receive frame error result: %s",av_err2str(AVERROR(result)))
        }
        int ret = av_read_frame(m_format_ctx, m_packet);
        while (ret == 0) {
            flag= false;
            if (m_packet->stream_index == m_stream_index) {
                int ret2=avcodec_send_packet(m_codec_ctx, m_packet);
                switch (ret2) {
                    case 0:
                        flag= true;
                        break;
                    case AVERROR_EOF: {
//                    av_packet_unref(m_packet);
                        LOG_ERROR(TAG, LogSpec(), "Decode error: %s", av_err2str(AVERROR_EOF));
//                        m_frame=NULL;
                        return AVERROR_EOF;//编码结束
                    }
                    case AVERROR(EAGAIN):
                        LOG_ERROR(TAG, LogSpec(), "Decode error: %s", av_err2str(AVERROR(EAGAIN)));
                        break;
                    case AVERROR(EINVAL):
                        LOG_ERROR(TAG, LogSpec(), "Decode error: %s", av_err2str(AVERROR(EINVAL)));
                        break;
                    case AVERROR(ENOMEM):
                        LOG_ERROR(TAG, LogSpec(), "Decode error: %s", av_err2str(AVERROR(ENOMEM)));
                        break;
                    default:
                        LOG_ERROR(TAG, LogSpec(), "Decode error: %s", av_err2str(ret2));
                        break;
                }
            }
            if(flag){
                break;
            }
            else {
                av_packet_unref(m_packet);
                ret = av_read_frame(m_format_ctx, m_packet);
            }
        }
        if(ret!=0){
            av_packet_unref(m_packet);
//            m_frame = NULL;
            LOGI(TAG, "ret = %s", av_err2str(AVERROR(ret)))
            return ret;
        }
    }
}
void BaseDecoder::ObtainTimeStamp() {
    if(m_frame->pkt_dts != AV_NOPTS_VALUE) {
        m_cur_t_s = m_packet->dts;
    } else if (m_frame->pts != AV_NOPTS_VALUE) {
        m_cur_t_s = m_frame->pts;
    } else {
        m_cur_t_s = 0;
    }

//    LOG_INFO(TAG, LogSpec(), "m_packet->dts %lld",m_packet->dts);
//    LOG_INFO(TAG, LogSpec(), "m_frame->pts %lld",m_frame->pts);
//    LOG_INFO(TAG, LogSpec(), "m_format_ctx->streams[m_stream_index]->time_base.num %d",m_format_ctx->streams[m_stream_index]->time_base.num);
//    LOG_INFO(TAG, LogSpec(), "m_format_ctx->streams[m_stream_index]->time_base.den %d",m_format_ctx->streams[m_stream_index]->time_base.den);
    m_cur_t_s = (int64_t)((m_cur_t_s * av_q2d(m_format_ctx->streams[m_stream_index]->time_base)) * 1000);
}
int64_t BaseDecoder::GetDuration() {
    return m_duration;
}
int64_t BaseDecoder::GetCurPos() {
    return m_cur_t_s;
}

