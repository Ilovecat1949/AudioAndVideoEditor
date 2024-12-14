//
// Created by deng on 2024/11/1.
//

#include "ReEncodingTask.h"
#include "../utils/logger.h"
#include <thread>
void ReEncodingTask::setInfo(TaskInfo* info) {
    this->info=info;
    this->m_env=info->env;
    this->m_dst_path=this->info->str_arr[0];
    this->m_src_path=this->info->str_arr[1];
    this->m_video_bit_rate=this->info->int64_arr[1];
    this->m_frame_rate.num=this->info->int_arr[1];
    this->video_buffer_size=this->info->int64_arr[2];
    this->audio_buffer_size=this->info->int64_arr[3];
}
int ReEncodingTask::taskInit() {
    int state=0;
    m_env->GetJavaVM(&m_jvm_for_thread);
    // 视频解码器
    m_video_decoder = new VideoDecoder(m_src_path);
    state=m_video_decoder->Init();
    if(state>=0){
        this->v_duration=m_video_decoder->GetDuration();
        this->video_flag= true;
        if(m_video_decoder->getImageBufferSize()>0){
            this->video_buffer_num=video_buffer_size/m_video_decoder->getImageBufferSize();
        }
        if(this->video_buffer_num<0){
            this->video_buffer_num=1;
        }
        LOGI(TAG,"video_buffer_num %lld  getImageBufferSize %d ",video_buffer_num,m_video_decoder->getImageBufferSize());
    }


    m_audio_decoder = new AudioDecoder(m_src_path);
    state=m_audio_decoder->Init();
    if(state>=0){
        this->a_duration=m_audio_decoder->GetDuration();
        this->audio_flag=true;
        if(m_audio_decoder->getSampleBufferSize()>0){
            this->audio_buffer_num=audio_buffer_size/m_audio_decoder->getSampleBufferSize();
        }
        if(this->audio_buffer_num<0){
            this->audio_buffer_num=1;
        }
        LOGI(TAG,"audio_buffer_num %lld  getSampleBufferSize %d ",audio_buffer_num,m_audio_decoder->getSampleBufferSize());
    }

    // 封装器
    if(video_flag || audio_flag) {
        m_mp4_muxer = new Mp4Muxer();
        state=m_mp4_muxer->Init(m_dst_path);
        if(state<0){
            return state;
        }
    }
    else{
        return -1;
    }
    // --------------------------视频配置--------------------------
    // 视频编码器
    if(this->video_flag) {
        m_v_encoder = new VideoEncoder(m_mp4_muxer);
        state=m_v_encoder->Init();
        if(state<0){
            return state;
        }
        //m_v_encoder->OpenEncoder(m_video_decoder->getAVCodecContext());
        if(m_video_bit_rate<0L){
            m_video_bit_rate=m_video_decoder->getBitRate();
        }
        if(m_frame_rate.num<0){
            m_frame_rate=m_video_decoder->getFrameRate();
        }

        m_v_encoder->setTargetPara(
                m_video_decoder->getWidth(),
                m_video_decoder->getHeight(),
                m_frame_rate,
                m_video_decoder->getPixelFormat(),
                m_video_bit_rate
                );
        state=m_v_encoder->OpenEncoder();
        if(state<0){
            return state;
        }
    }
    // 视频解码器
    //--------------------------音频配置--------------------------
    // 音频编码器
    if(this->audio_flag) {
        m_a_encoder = new AudioEncoder(m_mp4_muxer);
        state=m_a_encoder->Init();
        if(state<0){
            return state;
        }
        m_a_encoder->setTargetPara(
                m_audio_decoder->getSampleFmt(),
                m_audio_decoder->getSampleRate(),
                m_audio_decoder->getChannelLayout(),
                m_audio_decoder->getChannels(),
                m_audio_decoder->getBitRate(),
                m_audio_decoder->getProfile()
                );
        state=m_a_encoder->OpenEncoder();
        if(state<0){
            return state;
        }
        //m_a_encoder->OpenEncoder(m_audio_decoder->getAVCodecContext());
    }
    if(video_flag || audio_flag){
        state=m_mp4_muxer->Start();
        if(state<0){
            return state;
        }
    }
//    m_a_encoder->InitSwr();
    // 音频解码器
    if(this->audio_flag&&m_audio_decoder->getAVCodecContext()->frame_size!=m_a_encoder->getAVCodecContext()->frame_size){
        fifo_enable= true;
    }
    if(fifo_enable){
        fifo=new FFAudioFifo();
        fifo->init(m_a_encoder->getAVCodecContext());
    }
    return 0;
}
void ReEncodingTask::start() {
    ReEncodingTask* that=this;
    if(that->video_flag) {
        std::thread t1([that] {
            JNIEnv *env;
            //将线程附加到虚拟机，并获取env
            if (that->m_jvm_for_thread->AttachCurrentThread(&env, NULL) != JNI_OK) {
                LOG_ERROR("Video Decoder thread", that->LogSpec(), "Fail to Init decode thread");
                return;
            }
            int ret = that->m_video_decoder->DecodeOneFrame();
            while (ret == 0 && !that->cancel_flag[0]) {
                while (that->m_v_frames.size() > that->video_buffer_num) {
                    //av_usleep(1000);
                    std::this_thread::sleep_for(std::chrono::milliseconds(100));
                }
                //AVFrame *frame = that->m_video_decoder->getAVFrame();
                OneFrame *oneFrame=that->m_video_decoder->getOneFrame();
                if(oneFrame==NULL){
                    continue;
                }
                that->m_v_frames_lock.lock();
                //OneFrame *oneFrame = new OneFrame(frame, that->m_video_decoder->time_base());
                that->m_v_frames.push(oneFrame);
                that->m_v_frames_lock.unlock();
                ret = that->m_video_decoder->DecodeOneFrame();
//        that->job_flag[0]=ret;
            }
            //that->job_flag[0]==AVERROR_EOF &&
            if (!that->cancel_flag[0]) {
                that->m_v_frames_lock.lock();
                OneFrame *oneFrame = new OneFrame(NULL,NULL, that->m_video_decoder->time_base());
                that->m_v_frames.push(oneFrame);
                that->m_v_frames_lock.unlock();
            }
//        that->m_video_decoder->DoneDecode();
            that->m_jvm_for_thread->DetachCurrentThread();
            LOGI("Video Decoder thread", "Video Decoder thread Stop")
            that->job_flag[0] = 1;
        });
        t1.detach();
        std::thread t2([that] {
            JNIEnv *env;
            //将线程附加到虚拟机，并获取env
            if (that->m_jvm_for_thread->AttachCurrentThread(&env, NULL) != JNI_OK) {
                LOG_ERROR("Video Encoder thread", that->LogSpec(), "Fail to Init decode thread");
                return;
            }
            int state = 0;
            while (state == 0 && !that->cancel_flag[1]) {
                if (that->m_v_frames.size() > 0) {
                    that->m_v_frames_lock.lock();
                    OneFrame *oneFrame = that->m_v_frames.front();
                    that->m_v_frames.pop();
                    that->m_v_frames_lock.unlock();
                    if (oneFrame->frame != NULL) {
                        that->m_v_encoder->m_src_time_base = oneFrame->time_base;
                    }
                    state = that->m_v_encoder->EncodeOneFrame(oneFrame->frame);
                    delete oneFrame;
                    that->v_cur_time = that->m_v_encoder->getCurTime();
//                 that->job_flag[1]=state;
                }
            }
//        that->m_v_encoder->DoRelease();
            that->m_jvm_for_thread->DetachCurrentThread();
            LOGI("Video Encoder thread", "Video Encoder thread Stop %d", that->m_v_frames.size())
            that->job_flag[1] = 1;
        });
        t2.detach();
    }
    else{
        job_flag[0]=1;
        job_flag[1]=1;
    }
    if(that->audio_flag) {
        std::thread t3([that] {
            JNIEnv *env;
            //将线程附加到虚拟机，并获取env
            if (that->m_jvm_for_thread->AttachCurrentThread(&env, NULL) != JNI_OK) {
                LOG_ERROR("Audio Decoder thread", that->LogSpec(), "Fail to Init decode thread");
                return;
            }
            int ret = that->m_audio_decoder->DecodeOneFrame();
            while (ret == 0 && !that->cancel_flag[2]) {
                while (that->m_a_frames.size() > 100) {
                    //av_usleep(1000);
                    std::this_thread::sleep_for(std::chrono::milliseconds(100));
                }
//                AVFrame *frame = that->m_audio_decoder->getAVFrame();
//                OneFrame *oneFrame = new OneFrame(frame, that->m_audio_decoder->time_base());
                OneFrame *oneFrame=that->m_audio_decoder->getOneFrame();
                if(oneFrame==NULL){
                    continue;
                }
                that->m_a_frames_lock.lock();
                that->m_a_frames.push(oneFrame);
//                av_frame_free(&frame2);
                that->m_a_frames_lock.unlock();
                ret = that->m_audio_decoder->DecodeOneFrame();
//            that->job_flag[2]=ret;
            }
            //that->job_flag[2]==AVERROR_EOF &&
            if (!that->cancel_flag[2]) {
                that->m_a_frames_lock.lock();
                OneFrame *oneFrame = new OneFrame(NULL,NULL, that->m_audio_decoder->time_base());
                that->m_a_frames.push(oneFrame);
                that->m_a_frames_lock.unlock();
            }
//        that->m_audio_decoder->DoneDecode();
            that->m_jvm_for_thread->DetachCurrentThread();
            LOGI("Audio Decoder thread", "Audio Decoder thread Stop")
            that->job_flag[2] = 1;
        });
        t3.detach();
        std::thread t4([that] {
            JNIEnv *env;
            //将线程附加到虚拟机，并获取env
            if (that->m_jvm_for_thread->AttachCurrentThread(&env, NULL) != JNI_OK) {
                LOG_ERROR("Audio Encoder thread", that->LogSpec(), "Fail to Init decode thread");
                return;
            }
            int state = 0;
            while (state == 0 && !that->cancel_flag[3]) {
                if (that->m_a_frames.size() > 0) {
                    that->m_a_frames_lock.lock();
                    OneFrame *oneFrame = that->m_a_frames.front();
                    that->m_a_frames.pop();
                    that->m_a_frames_lock.unlock();
                    if (oneFrame->frame != NULL) {
                        that->m_a_encoder->m_src_time_base = oneFrame->time_base;
                    }
                    if (that->fifo_enable) {
                        LOG_INFO("Audio Encoder thread", that->LogSpec(), "that->fifo_enable");
                        int state2 = 0;
                        while (that->fifo->getSize() >=
                               that->m_a_encoder->getAVCodecContext()->frame_size) {//&&state2==0
//                            LOG_INFO("Audio Encoder thread", "fifo", "fifo->getSize() >=")
                            AVFrame *frame = NULL;
                            that->fifo->getAVFrame(frame);//state2=
                            if (frame != NULL) {
//                            state=that->m_a_encoder->EncodeOneFrame(frame);
//                                LOG_INFO("Audio Encoder thread", "fifo", "a_cur_time %lld",frame->pts)
//                                LOG_INFO("Audio Encoder thread", "fifo", "fifo->getSize() >=  frame != NULL")
                                that->m_a_encoder->EncodeOneFrame(frame);
                                av_frame_free(&frame);
                                that->a_cur_time = that->m_a_encoder->getCurTime();
                                LOG_INFO("Audio Encoder thread", "fifo", "a_cur_time %lld",that->a_cur_time)
                            }
                        }
                        if (oneFrame->frame != NULL) {
//                            LOG_INFO("Audio Encoder thread", that->LogSpec(), "add_samples_to_fifo ");
                            state = that->fifo->add_samples_to_fifo(oneFrame->frame->data,
                                                                    oneFrame->frame->nb_samples);
//                            LOG_INFO("Audio Encoder thread", that->LogSpec(), "add_samples_to_fifo %d",state);
                        } else {
                            while (that->fifo->getSize() > 0) {//&&state==0 &&state2==0
//                                LOG_INFO("Audio Encoder thread", that->LogSpec(),
//                                         "that->fifo->getSize()>0");
                                AVFrame *frame = NULL;
//                            state2=
                                that->fifo->getAVFrame(frame);
                                if (frame != NULL) {//frame!=NULL
                                    that->m_a_encoder->EncodeOneFrame(frame);
                                    av_frame_free(&frame);
                                    that->a_cur_time = that->m_a_encoder->getCurTime();
                                }
                            }
//                        LOG_ERROR("Audio Encoder thread", that->LogSpec(), "state :%d",state);
//                        if(state==0) {
                            state = that->m_a_encoder->EncodeOneFrame(NULL);
                            that->a_cur_time = that->m_a_encoder->getCurTime();
//                        }
                        }
                    } else {
                        state = that->m_a_encoder->EncodeOneFrame(oneFrame->frame);
                        that->a_cur_time = that->m_a_encoder->getCurTime();
                    }
                    delete oneFrame;
//                that->job_flag[3]=state;
                }
            }
//        that->m_a_encoder->DoRelease();
            that->m_jvm_for_thread->DetachCurrentThread();
            LOGI("Audio Encoder thread", "Audio Encoder thread Stop %d", that->m_a_frames.size())
            that->job_flag[3] = 1;
        });
        t4.detach();
    }
    else{
        job_flag[2]=1;
        job_flag[3]=1;
    }
}
void ReEncodingTask::cancel() {
    cancel_flag[0]= true;
    cancel_flag[1]= true;
    cancel_flag[2]= true;
    cancel_flag[3]= true;
}
void ReEncodingTask::release() {
    delete m_video_decoder;
    m_video_decoder=NULL;
    delete m_audio_decoder;
    m_audio_decoder=NULL;
    delete m_mp4_muxer;
    m_mp4_muxer=NULL;
    delete m_v_encoder;
    m_v_encoder=NULL;
    delete m_a_encoder;
    m_a_encoder=NULL;
    delete fifo;
    fifo=NULL;
    delete info;
    info=NULL;
    while(m_v_frames.size()>0){
        OneFrame * frame=m_v_frames.front();
        delete frame;
        m_v_frames.pop();
    }
    while(m_a_frames.size()>0){
        OneFrame * frame=m_a_frames.front();
        delete frame;
        m_a_frames.pop();
    }
}
float ReEncodingTask::getProgress() {
    LOG_INFO(TAG,"ReEncodingTask","v_duration %lld,a_duration %lld,v_cur_time %lld ,a_cur_time %lld",v_duration,a_duration,v_cur_time,a_cur_time);
    if((v_duration+a_duration)==0){
        return 0;
    }
    else{
        return (v_cur_time+a_cur_time)*1.0/(v_duration+a_duration);
    }
}
int ReEncodingTask::getState() {
    if(
            job_flag[0]==1
            &&job_flag[1]==1
            &&job_flag[2]==1
            &&job_flag[3]==1
            ){
        if( cancel_flag[0]
            &&cancel_flag[1]
            &&cancel_flag[2]
            &&cancel_flag[3]
        ){
            return 2;
        }
        return 1;
    }
    else{
        return 0;
    }
//    if(job_flag[1]==0
//    ||job_flag[3]==0
//    ) {
//        return 0;
//    }
//    else if(
//     job_flag[1]==1
//   &&job_flag[3]==1
//    ){
//        return 1;
//    }
//    else{
//        return -1;
//    }
}
ReEncodingTask::~ReEncodingTask() {
    release();
}