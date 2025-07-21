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
#include <fstream>
#include <cstring>
extern "C" int main(int argc, char **argv);
extern "C" void set_ffmpeg_cancel_flag(int flag);
extern "C" void set_exit_program_fun(void (*cb)(int ret));
extern "C" double get_ffmpeg_progress_time();
static jmp_buf buf2;
static std::mutex mtx2;
static int m_ret2=0;
static std::fstream ffmpeg_log_file2;
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
    const char * END_TAG="FFmpegTask END";
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
    const char * getAVLevel(int level){
        switch (level){
            case AV_LOG_QUIET:
                return "AV_LOG_QUIET";
                break;
            case AV_LOG_PANIC:
                return "AV_LOG_PANIC";
                break;
            case AV_LOG_FATAL:
                return "AV_LOG_FATAL";
                break;
            case AV_LOG_ERROR:
                return "AV_LOG_ERROR";
                break;
            case AV_LOG_WARNING:
                return "AV_LOG_WARNING";
                break;
            case AV_LOG_INFO:
                return "AV_LOG_INFO";
                break;
            case AV_LOG_VERBOSE:
                return "AV_LOG_VERBOSE";
                break;
            case AV_LOG_DEBUG:
                return "AV_LOG_DEBUG";
                break;
            case AV_LOG_TRACE:
                return "AV_LOG_TRACE";
                break;
            case AV_LOG_MAX_OFFSET:
                return "AV_LOG_MAX_OFFSET";
                break;
            default :
                return "UNKNOWN";
        }
    }
    int cancel_flag=0;
    bool startsWith(const char* mainString, const char* prefix) {
        size_t mainLen = strlen(mainString);
        size_t prefixLen = strlen(prefix);

        if (prefixLen > mainLen) {
            return false; // Prefix is longer than the main string
        }

        return strncmp(mainString, prefix, prefixLen) == 0;
    }
    bool endsWith(const char* mainString, const char* suffix) {
        size_t mainLen = strlen(mainString);
        size_t suffixLen = strlen(suffix);

        if (suffixLen > mainLen) {
            return false; // Suffix is longer than the main string
        }

        // Calculate the starting position in mainString to compare
        // We cast to char* because mainString is const char* and we're doing pointer arithmetic
        const char* compareStart = mainString + (mainLen - suffixLen);

        return strcmp(compareStart, suffix) == 0;
    }
    const char* error_messages[4]={
            "Option ",
            " not found.\n",
            "Too many packets buffered for output stream ",
            "aborting.\n"
    };
    bool isErrorLog(const char* str){
        return
                (startsWith(str,error_messages[0]) && endsWith(str,error_messages[1]))
              ||startsWith(str,error_messages[2])
              ||strcmp(str,error_messages[3])
        ;
    }
};


#endif //AUDIOANDVIDEOEDITOR_FFMPEGSERVICETASK_H
