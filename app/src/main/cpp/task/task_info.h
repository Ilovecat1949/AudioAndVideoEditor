//
// Created by deng on 2024/11/1.
//

#ifndef AUDIOANDVIDEOEDITOR_TASK_INFO_H
#define AUDIOANDVIDEOEDITOR_TASK_INFO_H
#include <jni.h>
class TaskInfo{
public:
    int int_len=0;
    int * int_arr=NULL;
    int int64_len=0;
    int64_t * int64_arr=NULL;
    int float_len=0;
    float * float_arr=NULL;
    int str_len=0;
    char ** str_arr=NULL;
    JNIEnv *env;
    ~TaskInfo(){
        if(int_arr!=NULL){
            delete [] int_arr;
            int_len=0;
            int_arr=NULL;
        }
        if(int64_arr!=NULL){
            delete [] int64_arr;
            int64_arr=NULL;
            int64_len=0;
        }
        if(float_arr!=NULL){
            delete [] float_arr;
            float_arr=NULL;
            float_len=0;
        }
        for(int i=0;i<str_len;i++){
            delete[] str_arr[i];
        }
        str_len=0;
    }
};
#endif //AUDIOANDVIDEOEDITOR_TASK_INFO_H
