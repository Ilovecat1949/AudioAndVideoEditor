//
// Created by deng on 2024/12/17.
//

#ifndef AUDIOANDVIDEOEDITOR_REPACKAGINGTASK_H
#define AUDIOANDVIDEOEDITOR_REPACKAGINGTASK_H

#include "task_info.h"
#include "task.h"
#include <jni.h>
extern "C" {
#include <libavformat/avformat.h>
};
class RePackagingTask : public Task {
public:
    void setInfo(TaskInfo * info) override ;
    int taskInit() override ;
    void start() override ;
    void cancel() override ;
    void release() override ;
    float getProgress() override ;
    int getState() override ;
    ~RePackagingTask();
private:
    TaskInfo *info;
    const char * TAG="RePackagingTask";
    char* m_src_path;
    char* m_dst_path;
    JNIEnv *m_env;
    JavaVM *m_jvm_for_thread = NULL;
    int job_flag=0;
    int cancel_flag=0;
    AVFormatContext *m_in_format_cxt = NULL;
    AVFormatContext *m_out_format_cxt = NULL;
    void Write(AVPacket pkt);
    int seekFrame(int64_t time, int stream_index, int seekFlag);
    int compare_ts(int stream_index,int64_t pts,int64_t time);
    void adjustTime(AVPacket &pkt,int64_t time);
    int64_t m_duration=0;
    int64_t m_cur_time=0;
    int64_t start_time=-1;
    int64_t end_time=-1;
};


#endif //AUDIOANDVIDEOEDITOR_REPACKAGINGTASK_H
