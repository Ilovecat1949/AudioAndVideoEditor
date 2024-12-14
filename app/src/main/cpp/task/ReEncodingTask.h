//
// Created by deng on 2024/11/1.
//

#ifndef AUDIOANDVIDEOEDITOR_REENCODINGTASK_H
#define AUDIOANDVIDEOEDITOR_REENCODINGTASK_H


#include <jni.h>
#include "task_info.h"
#include "task.h"
#include "../media/decoder/video/VideoDecoder.h"
#include "../media/muxer/Mp4Muxer.h"
#include "../media/decoder/audio/AudioDecoder.h"
#include "../media/encoder/video/VideoEncoder.h"
#include "../media/encoder/audio/AudioEncoder.h"
#include "../media/fifo/FFAudioFifo.h"
#include "../utils/one_frame.h"
#include <queue>
#include <mutex>
class ReEncodingTask: public Task {
public:
    void setInfo(TaskInfo * info) override ;
    int taskInit() override ;
    void start() override ;
    void cancel() override ;
    void release() override ;
    float getProgress() override ;
    int getState() override ;
    ~ReEncodingTask();
private:
    TaskInfo *info;
    const char * TAG="ReEncodingTask";
    VideoDecoder *m_video_decoder = NULL;
    AudioDecoder *m_audio_decoder = NULL;
    Mp4Muxer *m_mp4_muxer = NULL;
    VideoEncoder *m_v_encoder = NULL;
    AudioEncoder *m_a_encoder = NULL;
    FFAudioFifo *fifo=NULL;
    std::queue<OneFrame *> m_v_frames;
    std::queue<OneFrame *> m_a_frames;
    std::mutex m_v_frames_lock;
    std::mutex m_a_frames_lock;
    char* m_src_path;
    char* m_dst_path;
    JavaVM *m_jvm_for_thread = NULL;
    int job_flag[4]={0,0,0,0};
    bool cancel_flag[4]={false,false,false,false};
    const char *const LogSpec() {
        return "ReEncodingTask";
    };
    bool fifo_enable= false;
    bool audio_flag= false;
    bool video_flag= false;
    JNIEnv *m_env;
    int64_t v_duration=0;
    int64_t a_duration=0;
    int64_t v_cur_time=0;
    int64_t a_cur_time=0;
    int64_t m_video_bit_rate=-1;
    AVRational m_frame_rate={-1,1};
    int64_t video_buffer_size=0L;
    int64_t video_buffer_num=5L;
    int64_t audio_buffer_size=0L;
    int64_t audio_buffer_num=5L;
};


#endif //AUDIOANDVIDEOEDITOR_REENCODINGTASK_H
