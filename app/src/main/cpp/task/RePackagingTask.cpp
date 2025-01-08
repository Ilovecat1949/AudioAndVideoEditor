//
// Created by deng on 2024/12/17.
//

#include "RePackagingTask.h"
#include "../utils/logger.h"
#include <thread>
void RePackagingTask::setInfo(TaskInfo *info) {
    this->info=info;
    this->m_env=info->env;
    this->m_dst_path=this->info->str_arr[0];
    this->m_src_path=this->info->str_arr[1];
    this->start_time=this->info->int64_arr[1];
    this->end_time=this->info->int64_arr[2];
}

int RePackagingTask::taskInit() {
    m_env->GetJavaVM(&m_jvm_for_thread);
    // 打开文件
    LOGI(TAG, "Open file: %s", this->m_src_path);
    if ((avformat_open_input(&m_in_format_cxt, this->m_src_path, NULL, NULL)) < 0) {
        LOGE(TAG, "Fail to open input file")
        return -1;
    }
    // 获取音视频参数
    if ((avformat_find_stream_info(m_in_format_cxt, 0)) < 0) {
        LOGE(TAG, "Fail to retrieve input stream information")
        return -1;
    }
    // 初始化输出上下文
    if (avformat_alloc_output_context2(&m_out_format_cxt, NULL, NULL, this->m_dst_path) < 0) {
        return -1;
    }

    // 查找原视频所有媒体流
    for (int i = 0; i < m_in_format_cxt->nb_streams; ++i) {
        // 获取媒体流
        AVStream *in_stream = m_in_format_cxt->streams[i];

        // 为目标文件创建输出流
        AVStream *out_stream = avformat_new_stream(m_out_format_cxt, NULL);
        if (!out_stream) {
            LOGE(TAG, "Fail to allocate output stream")
            return -1;
        }

        // 复制原视频数据流参数到目标输出流
        if (avcodec_parameters_copy(out_stream->codecpar, in_stream->codecpar) < 0) {
            LOGE(TAG, "Fail to copy input context to output stream")
            return -1;
        }
    }

    // 打开目标文件
    if (avio_open(&m_out_format_cxt->pb, this->m_dst_path, AVIO_FLAG_WRITE) < 0) {
        LOGE(TAG, "Could not open output file %s ", this->m_dst_path);
        return -1;
    }

    // 写入文件头信息
    if (avformat_write_header(m_out_format_cxt, NULL) < 0) {
        LOGE(TAG, "Error occurred when opening output file");
        return -1;
    } else {
        LOGE(TAG, "Write file header success");
    }
    m_duration=(int64_t)((float)m_in_format_cxt->duration/AV_TIME_BASE * 1000);
    return 0;
}

void RePackagingTask::start() {
    RePackagingTask *that=this;
    std::thread t1([that] {
        JNIEnv *env;
        if (that->m_jvm_for_thread->AttachCurrentThread(&env, NULL) != JNI_OK) {
            return;
        }
        LOGE(that->TAG, "Start repacking ....")
        AVPacket pkt;
        if(that->start_time!=-1){
            int error=that->seekFrame(that->start_time,-1,AVSEEK_FLAG_BACKWARD);
            if(error<0){
                that->job_flag=-1;
                LOGE(that->TAG, "End of video，write trailer")
                // 释放数据帧和相关资源
                av_packet_unref(&pkt);
                // 读取完毕，写入结尾信息
                av_write_trailer(that->m_out_format_cxt);
                return;
            }
        }
        while (!that->cancel_flag) {
            // 读取数据
            if (av_read_frame(that->m_in_format_cxt, &pkt)) {
                LOGE(that->TAG, "End of video，write trailer")
                // 释放数据帧和相关资源
                av_packet_unref(&pkt);
                // 读取完毕，写入结尾信息
                av_write_trailer(that->m_out_format_cxt);
                break;
            }
            if(that->end_time!=-1){
                if(that->compare_ts(pkt.stream_index,pkt.pts,that->end_time)==1){
                    LOGE(that->TAG, "End of video，write trailer")
                    // 释放数据帧和相关资源
                    av_packet_unref(&pkt);
                    // 读取完毕，写入结尾信息
                    av_write_trailer(that->m_out_format_cxt);
                    break;
                }
            }
            if(that->start_time!=-1){
                if(that->compare_ts(pkt.stream_index,pkt.pts,that->start_time)==-1){
                    continue;
                }
                that->adjustTime(pkt,that->start_time);
            }
            // 写入一帧数据
            that->Write(pkt);
            // 这里先不要释放资源，否则会导致写入异常，在文件读取完毕后释放
            // av_packet_unref(&pkt);
        }
        that->job_flag=1;
    });
    t1.detach();
}

void RePackagingTask::Write(AVPacket pkt) {
    // 获取数据对应的输入/输出流
    AVStream *in_stream = m_in_format_cxt->streams[pkt.stream_index];
    AVStream *out_stream = m_out_format_cxt->streams[pkt.stream_index];

    // 转换时间基对应的 PTS/DTS
    int rounding = (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX);
    pkt.pts = av_rescale_q_rnd(pkt.pts, in_stream->time_base, out_stream->time_base,
                               (AVRounding)rounding);
    pkt.dts = av_rescale_q_rnd(pkt.dts, in_stream->time_base, out_stream->time_base,
                               (AVRounding)rounding);
    pkt.duration = av_rescale_q(pkt.duration, in_stream->time_base, out_stream->time_base);
    pkt.pos = -1;
    // 将数据写入目标文件
    m_cur_time = (int64_t) (pkt.pts *
                                av_q2d(out_stream->time_base) *
                                1000);
    int ret = av_interleaved_write_frame(m_out_format_cxt, &pkt);
    if (ret < 0) {
        LOGE(TAG, "Error to muxing packet: %x", ret)
    }
}

void RePackagingTask::cancel() {
    cancel_flag=1;
}

float RePackagingTask::getProgress() {
    //LOGI(TAG,"m_duration:%lld,m_cur_time:%lld",m_duration,m_cur_time)
    if(m_duration==0){
        return 0;
    }
    else{
        return m_cur_time*1.0/m_duration;
    }
}

int RePackagingTask::getState() {
    if(job_flag==0){
        return 0;
    }
    else if(cancel_flag==1 && job_flag==1){
        return 2;
    }
    else if(job_flag==-1){
        return -1;
    }
    else{
        return 1;
    }
}

void RePackagingTask::release() {
    delete info;
    info=NULL;
    LOGE(TAG, "Finish repacking, release resources")
    // 关闭输入
    if (m_in_format_cxt) {
        avformat_close_input(&m_in_format_cxt);
    }

    // 关闭输出
    if (m_out_format_cxt) {
        avio_close(m_out_format_cxt->pb);
        avformat_free_context(m_out_format_cxt);
    }
}

RePackagingTask::~RePackagingTask() {
    release();
}

int RePackagingTask::seekFrame(int64_t time, int stream_index, int seekFlag) {
    int64_t timestamp = 0;
    if(stream_index == -1)
    {
        timestamp = time * AV_TIME_BASE;
    }
    else
    {
        AVStream *in_stream = m_in_format_cxt->streams[stream_index];
        timestamp =  time / av_q2d(in_stream->time_base);
    }
    int error = av_seek_frame(m_in_format_cxt, stream_index, timestamp, seekFlag);
    return error;
}

int RePackagingTask::compare_ts(int stream_index,int64_t pts, int64_t time) {
    AVStream *in_stream = m_in_format_cxt->streams[stream_index];
    AVRational time_base = {1, AV_TIME_BASE};
    return av_compare_ts(pts, in_stream->time_base, time * AV_TIME_BASE, time_base);
}

void RePackagingTask::adjustTime(AVPacket &pkt, int64_t time) {
    AVStream *in_stream = m_in_format_cxt->streams[pkt.stream_index];
    int rounding = (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX);
    AVRational time_base = {1, AV_TIME_BASE};
    int64_t timestamp = av_rescale_q_rnd(time * AV_TIME_BASE, time_base, in_stream->time_base,
                                         (AVRounding)rounding);
    LOGI(TAG,"pkt.pts:%f",pkt.pts*av_q2d(in_stream->time_base))
    pkt.pts=pkt.pts-timestamp;
    pkt.dts=pkt.dts-timestamp;
}



