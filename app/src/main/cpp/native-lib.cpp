

#include "utils/logger.h"
#include "task/TasksFactory.h"
#include "task/AudioAndVideoInfo.h"
#include "task/FFmpegInfo.h"

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavfilter/avfilter.h>
#include <libavcodec/jni.h>
#include <jni.h>
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    av_jni_set_java_vm(vm, reserved);
    LOG_INFO("JNI_OnLoad", "--------", "");
    return JNI_VERSION_1_4;
}
JNIEXPORT jstring JNICALL
Java_com_example_audioandvideoeditor_MainActivity_ffmpegInfo(JNIEnv *env, jobject thiz) {
    char info[40000] = {0};
    AVCodec *c_temp = av_codec_next(NULL);
    while (c_temp != NULL) {
        if (c_temp->decode != NULL) {
            sprintf(info, "%sdecode:", info);
        } else {
            sprintf(info, "%sencode:", info);
        }
        switch (c_temp->type) {
            case AVMEDIA_TYPE_VIDEO:
                sprintf(info, "%s(video):", info);
                break;
            case AVMEDIA_TYPE_AUDIO:
                sprintf(info, "%s(audio):", info);
                break;
            default:
                sprintf(info, "%s(other):", info);
                break;
        }
        sprintf(info, "%s[%s]\n", info, c_temp->name);
        c_temp = c_temp->next;
    }
//    jstring jstr=m_env->NewStringUTF(info);
    return env->NewStringUTF(info);
//    m_env->GetStr
}
JNIEXPORT jlong JNICALL
Java_com_example_audioandvideoeditor_services_TasksService_initTasksFactory(JNIEnv *env,jobject thiz) {
    TasksFactory *m_taskFactory=new TasksFactory();
    return (jlong)m_taskFactory;
}
JNIEXPORT jint JNICALL
Java_com_example_audioandvideoeditor_services_TasksService_createAndStartTask(JNIEnv *env,
                                                                              jobject thiz,
                                                                              jlong m_tasks_factory,
                                                                              jintArray int_arr,
                                                                              jlongArray long_arr,
                                                                              jfloatArray float_arr,
                                                                              jobjectArray str_arr)
                                                                              {
    TaskInfo *info=new TaskInfo();
    info->env=env;
    if(env->GetArrayLength(int_arr)>0) {
        info->int_len= env->GetArrayLength(int_arr);
        info->int_arr = new int[info->int_len];
        jint *jint_arr = env->GetIntArrayElements(int_arr, JNI_FALSE);
//    env->GetIntArrayRegion(int_arr,0,info->int_len,jint_arr);
        for (int i = 0; i < info->int_len; i++) {
            info->int_arr[i] = jint_arr[i];
            LOG_INFO("TaskInfo", "int:", "%d", info->int_arr[i]);
        }
        env->ReleaseIntArrayElements(int_arr, jint_arr, JNI_ABORT);
    }

    if(env->GetArrayLength(long_arr)>0) {
        info->int64_len= env->GetArrayLength(long_arr);
        info->int64_arr = new int64_t[info->int64_len];
        jlong *jlong_arr = env->GetLongArrayElements(long_arr, JNI_FALSE);
        for (int i = 0; i < info->int64_len; i++) {
            info->int64_arr[i] = jlong_arr[i];
            LOG_INFO("TaskInfo", "int64:", "%lld", info->int64_arr[i]);
        }
        env->ReleaseLongArrayElements(long_arr, jlong_arr, JNI_ABORT);
    }

    if(env->GetArrayLength(float_arr)>0) {
        info->float_len = env->GetArrayLength(float_arr);
        info->float_arr = new float[info->float_len];
        jfloat *jfloat_arr = env->GetFloatArrayElements(float_arr, JNI_FALSE);
        for (int i = 0; i < info->float_len; i++) {
            info->float_arr[i] = jfloat_arr[i];
            LOG_INFO("TaskInfo", "float:", "%f", info->float_arr[i]);
        }
        env->ReleaseFloatArrayElements(float_arr, jfloat_arr, JNI_ABORT);
    }

    if(env->GetArrayLength(str_arr)>0) {
        info->str_len=env->GetArrayLength(str_arr);
        info->str_arr = new char *[info->str_len];
        for (int i = 0; i < info->str_len; i++) {
            jstring jstr = static_cast<jstring>(env->GetObjectArrayElement(str_arr, i));
            const char *jstr2 = env->GetStringUTFChars(jstr, NULL);
            int len = strlen(jstr2);
            info->str_arr[i] = new char[len + 1];
            strcpy(info->str_arr[i], jstr2);
            env->ReleaseStringUTFChars(jstr, jstr2);
            LOG_INFO("TaskInfo", "str:", "%s", info->str_arr[i]);
        }
    }
    TasksFactory *tasks_factory=(TasksFactory *)m_tasks_factory;
    int state=tasks_factory->createTask(info);
    if(state<0){
        return -1;
    }
    tasks_factory->start(info->int64_arr[0]);
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_example_audioandvideoeditor_services_TasksService_getTaskState(JNIEnv *env, jobject thiz,
                                                                        jlong m_tasks_factory,
                                                                        jlong task_id) {
    TasksFactory *tasks_factory=(TasksFactory *)m_tasks_factory;
    return tasks_factory->getState(task_id);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_audioandvideoeditor_services_TasksService_releaseTask(JNIEnv *env, jobject thiz,
                                                                       jlong m_tasks_factory,
                                                                       jlong task_id) {
    TasksFactory *tasks_factory=(TasksFactory *)m_tasks_factory;
    tasks_factory->release(task_id);
}

JNIEXPORT jfloat JNICALL
Java_com_example_audioandvideoeditor_services_TasksService_getProgress(JNIEnv *env, jobject thiz,
                                                                       jlong m_tasks_factory,
                                                                       jlong task_id) {
    TasksFactory *tasks_factory=(TasksFactory *)m_tasks_factory;
    LOG_INFO("TasksService_getProgress","TasksService_getProgress","getProgress %f",tasks_factory->getProgress(task_id))
    return tasks_factory->getProgress(task_id);
}
JNIEXPORT jstring JNICALL
Java_com_example_audioandvideoeditor_services_TasksService_getAudioAndVideoStrInfo(JNIEnv *env,
                                                                                   jobject thiz,
                                                                                   jstring path) {
    const char *path2 = env->GetStringUTFChars(path, NULL);
    AudioAndVideoInfo *av_info=new AudioAndVideoInfo();
    av_info->Init(path2);
    char * info_str=av_info->getStrInfo();
    jstring info_jstr=env->NewStringUTF(info_str);
    delete [] info_str;
    delete av_info;
    env->ReleaseStringUTFChars(path, path2);
    return info_jstr;
}
JNIEXPORT void JNICALL
Java_com_example_audioandvideoeditor_services_TasksService_cancelTask(JNIEnv *env, jobject thiz,
                                                                      jlong m_tasks_factory,
                                                                      jlong task_id) {
    TasksFactory *tasks_factory=(TasksFactory *)m_tasks_factory;
    tasks_factory->cancel(task_id);
}
JNIEXPORT jstring JNICALL
Java_com_example_audioandvideoeditor_services_TasksService_getFFmpegStrInfo(JNIEnv *env,
                                                                                jobject thiz,
                                                                                jint info_type) {
    FFmpegInfo info;
    char * info_str=info.getStrFFmpegInfo(info_type);
    jstring info_jstr=env->NewStringUTF(info_str);
    delete [] info_str;
    return info_jstr;
}
}



