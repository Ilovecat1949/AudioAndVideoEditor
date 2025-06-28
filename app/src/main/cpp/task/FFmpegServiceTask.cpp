//
// Created by deng on 2025/2/3.
//

#include "FFmpegServiceTask.h"
#include "../utils/logger.h"

void FFmpegServiceTask::setInfo(TaskInfo *info) {
    this->info=info;
    this->m_env=info->env;
    this->argc=this->info->int_arr[1];
    this->argv=new char *[this->argc+1];
    for(int i=0;i<this->argc;i++){
        this->argv[i]=this->info->str_arr[i+2];
    }
    this->argv[this->argc]=NULL;
    if(this->info->int_arr [0]==3) {
        target_duration =this->info->int64_arr[1];
    }
}

int FFmpegServiceTask::taskInit() {
    m_env->GetJavaVM(&m_jvm_for_thread);
    ffmpeg_log_file2.open(this->info->str_arr[1], std::ios::app);
    return 0;
}

void FFmpegServiceTask::start() {
    FFmpegServiceTask *that=this;
    std::thread t1([that] {
        JNIEnv *env;
        if (that->m_jvm_for_thread->AttachCurrentThread(&env, NULL) != JNI_OK) {
            return;
        }
        LOGI(that->TAG, "Start FFmpegServiceTask ....")
        that->ffmpeg_exec();
    });
    t1.detach();
}

void FFmpegServiceTask::cancel() {

}

void FFmpegServiceTask::release() {
    if(ffmpeg_log_file2.is_open()){
        ffmpeg_log_file2.write("\n", sizeof(char));
        ffmpeg_log_file2.write(END_TAG, sizeof(char)* strlen(END_TAG));
        ffmpeg_log_file2.flush();
        ffmpeg_log_file2.close();
        ffmpeg_log_file2.clear(std::ios::goodbit);
    }
    delete info;
    info=NULL;
    delete log_str;
    log_str=NULL;
}

float FFmpegServiceTask::getProgress() {
    mtx2.lock();
    if(log_str!=NULL){
        if(this->containsTime(log_str)){
          if(target_duration>0){
              //LOGI(TAG,"time:%lld,duration %lld",extractTimeInMilliseconds(log_str),target_duration)
              progress_num=extractTimeInMilliseconds(log_str)*1.0/target_duration;
              //LOGI(TAG,"time:%lld,duration %lld,progress_num:%f",extractTimeInMilliseconds(log_str),target_duration,progress_num);
          }
        }
    }
    mtx2.unlock();
    return progress_num;
}

int FFmpegServiceTask::getState() {
    return job_flag;
}

FFmpegServiceTask::~FFmpegServiceTask() {
    release();
}

void FFmpegServiceTask::ffmpeg_exec() {
    av_log_set_callback([](void *avcl, int level, const char *fmt, va_list vl)
                        {
                            mtx2.lock();
                            AVBPrint part;
                            av_bprint_init(&part, 0, 65536);
                            av_vbprintf(&part, fmt, vl);
                            if(level<=AV_LOG_INFO){
                                __android_log_print(ANDROID_LOG_INFO, "ffmpeg", "%d:%s",level, part.str);
                                delete log_str;
                                log_str=new char[strlen(part.str)+1];
                                strcpy(log_str,part.str);
                                ffmpeg_log_file2.write(part.str, part.len);
                                ffmpeg_log_file2.flush();
                            }
                            av_bprint_finalize(&part, NULL);
                            mtx2.unlock();
                        }
    );
    set_exit_program_fun([](int ret){
        m_ret2=ret;
        longjmp(buf2,-1);
    });
    if(setjmp(buf2)!=0){
        job_flag=1;
        LOGI(TAG, "FFmpegServiceTask EXIT  %d",m_ret2);
        return ;
    }
    main(argc, argv);
}







