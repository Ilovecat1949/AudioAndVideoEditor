//
// Created by deng on 2025/2/3.
//

#ifndef AUDIOANDVIDEOEDITOR_FFMPEGSERVICETASK_H
#define AUDIOANDVIDEOEDITOR_FFMPEGSERVICETASK_H

#include <setjmp.h>
#include "task.h"
#include <thread>
#include <string>
#include <sstream>
extern "C" int main(int argc, char **argv);
extern "C" void set_exit_program_fun(void (*cb)(int ret));
static jmp_buf buf2;
static std::mutex mtx2;
static int m_ret2=0;
static char * log_str=NULL;
extern "C" {
#include "libavutil/log.h"
#include "libavutil/bprint.h"
}
class FFmpegServiceTask: public Task {
public:
    void setInfo(TaskInfo * info) override ;
    int taskInit() override ;
    void start() override ;
    void cancel() override ;
    void release() override ;
    float getProgress() override ;
    int getState() override ;
    ~FFmpegServiceTask();
private:
    const char * TAG="FFmpegServiceTask";
    TaskInfo *info;
    JNIEnv *m_env;
    JavaVM *m_jvm_for_thread = NULL;
    int argc;
    char **argv;
    int job_flag=0;
    float progress_num=0;
    int64_t target_duration=0;
    void ffmpeg_exec();
    // 函数用于将时间字符串（格式：HH:MM:SS.mmm）转换为毫秒
    int64_t timeToMilliseconds(const std::string& timeStr) {
        int hours, minutes, seconds, milliseconds;
        char discard; // 用来跳过冒号和点号
        std::istringstream timeStream(timeStr);
        timeStream >> hours >> discard >> minutes >> discard >> seconds >> discard >> milliseconds;
        return hours * 3600000L + minutes * 60000L + seconds * 1000L + milliseconds;
    }
    int64_t extractTimeInMilliseconds(const char* input) {
        const char* timeStart = strstr(input, "time=");
        if (timeStart == nullptr) {
            return 0;
        }
        // 跳过 "time=" 字符串
        timeStart += 5;
        // 找到时间字符串结束位置
        const char* timeEnd = strstr(timeStart, " ");
        if (timeEnd == nullptr) {
            return 0;
        }
        // 计算时间字符串长度
        size_t timeLength = timeEnd - timeStart;
        // 截取时间字符串
        std::string timeStr(timeStart, timeLength);
        // 将时间字符串转换为毫秒
        return timeToMilliseconds(timeStr);
    }
    bool containsTime(const char* input) {
        if (input == nullptr) {
            return false; // 如果输入为空指针，直接返回 false
        }
        // 使用 strstr 查找 "time=" 子串
        const char* timeMarker = "time=";
        const char* found = strstr(input, timeMarker);
        // 如果 found 不是 nullptr，说明找到了 "time="
        return found != nullptr;
    }
};


#endif //AUDIOANDVIDEOEDITOR_FFMPEGSERVICETASK_H
