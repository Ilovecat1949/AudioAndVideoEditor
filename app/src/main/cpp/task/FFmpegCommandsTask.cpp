//
// Created by deng on 2024/12/28.
//

#include "FFmpegCommandsTask.h"
#include "../utils/logger.h"


void FFmpegCommandsTask::setInfo(TaskInfo *info) {
    this->info=info;
    this->m_env=info->env;
    this->argc=this->info->int_arr[1];
    this->argv=new char *[this->argc+1];
    for(int i=0;i<this->argc;i++){
        this->argv[i]=this->info->str_arr[i+1];
    }
    this->argv[this->argc]=NULL;
}

int FFmpegCommandsTask::taskInit() {
    m_env->GetJavaVM(&m_jvm_for_thread);
    ffmpeg_log_file.open(this->info->str_arr[0], std::ios::app);
    cancel_flag=0;
    set_ffmpeg_cancel_flag(cancel_flag);
    return 0;
}

void FFmpegCommandsTask::cancel() {
    cancel_flag=1;
    set_ffmpeg_cancel_flag(cancel_flag);
}

void FFmpegCommandsTask::release() {
   if(ffmpeg_log_file.is_open()){
       ffmpeg_log_file.write("\n", sizeof(char));
//       ffmpeg_log_file.write(END_TAG, sizeof(char)* strlen(END_TAG));
       char end_info[100] = {0};
       sprintf(end_info, "FFmpegCommandsTask EXIT  %d",m_ret);
       ffmpeg_log_file.write(end_info, sizeof(char)* strlen(end_info));
       ffmpeg_log_file.flush();
       ffmpeg_log_file.close();
       ffmpeg_log_file.clear(std::ios::goodbit);
   }
   delete info;
   info=NULL;
}

float FFmpegCommandsTask::getProgress() {
    return get_ffmpeg_progress_time();
}

int FFmpegCommandsTask::getState() {
    if(job_flag!=0 && cancel_flag!=0){
        return 2;
    }
    else if(job_flag!=0 && m_ret==1){
        return -1;
    }
    else{
        return job_flag;
    }
}

FFmpegCommandsTask::~FFmpegCommandsTask() {
   release();
}

void FFmpegCommandsTask::start() {
    FFmpegCommandsTask *that=this;
    std::thread t1([that] {
        JNIEnv *env;
        if (that->m_jvm_for_thread->AttachCurrentThread(&env, NULL) != JNI_OK) {
            return;
        }
        LOGI(that->TAG, "Start FFmpegCommandsTask ....")
        that->ffmpeg_exec();
    });
    t1.detach();
}
void FFmpegCommandsTask::ffmpeg_exec() {
    av_log_set_callback([](void *avcl, int level, const char *fmt, va_list vl)
                        {
//                            mtx.lock();
                            AVBPrint part;
                            av_bprint_init(&part, 0, 65536);
                            av_vbprintf(&part, fmt, vl);
                            if(level<=AV_LOG_INFO) {
                                __android_log_print(ANDROID_LOG_INFO, "ffmpeg", "%d:%s", level,
                                                    part.str);
                                ffmpeg_log_file.write(part.str, part.len);
                                ffmpeg_log_file.flush();
                            }
                            av_bprint_finalize(&part, NULL);
//                            mtx.unlock();
                        }
    );
    set_exit_program_fun([](int ret){
        m_ret=ret;
        longjmp(buf,-1);
    });
    if(setjmp(buf)!=0){
        job_flag=1;
        LOGI(TAG, "FFmpegCommandsTask EXIT  %d",m_ret);
        return ;
    }
    main(argc, argv);
}


