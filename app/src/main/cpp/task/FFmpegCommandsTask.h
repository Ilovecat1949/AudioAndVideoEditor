//
// Created by deng on 2024/12/28.
//

#ifndef AUDIOANDVIDEOEDITOR_FFMPEGCOMMANDSTASK_H
#define AUDIOANDVIDEOEDITOR_FFMPEGCOMMANDSTASK_H
#include <setjmp.h>
#include<fstream>
#include "task.h"
#include <thread>
extern "C" int main(int argc, char **argv);
extern "C" void set_exit_program_fun(void (*cb)(int ret));
static jmp_buf buf;
static std::fstream ffmpeg_log_file;
static std::mutex mtx;
static int m_ret=0;
extern "C" {
#include "libavutil/log.h"
#include "libavutil/bprint.h"
}
class FFmpegCommandsTask: public Task {
public:
    void setInfo(TaskInfo * info) override ;
    int taskInit() override ;
    void start() override ;
    void cancel() override ;
    void release() override ;
    float getProgress() override ;
    int getState() override ;
    ~FFmpegCommandsTask();
private:
    const char * TAG="FFmpegCommandsTask";
    const char * END_TAG="FFmpegCommandsTask END";
    TaskInfo *info;
    JNIEnv *m_env;
    JavaVM *m_jvm_for_thread = NULL;
    int argc;
    char **argv;
    int job_flag=0;
    void ffmpeg_exec();
};


#endif //AUDIOANDVIDEOEDITOR_FFMPEGCOMMANDSTASK_H
