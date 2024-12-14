//
// Created by deng on 2024/11/1.
//

#ifndef AUDIOANDVIDEOEDITOR_TASK_H
#define AUDIOANDVIDEOEDITOR_TASK_H

#include "task_info.h"

class Task{
public:
//virtual  void setInfo(TaskInfo * info)=0;
//virtual  void taskInit()=0;
//virtual  void start()=0;
//virtual  void cancel()=0;
//virtual  void release()=0;
//virtual  float getProgress()=0;
//virtual int getState()=0;
    virtual  void setInfo(TaskInfo * info){};
    virtual  int taskInit(){};
    virtual  void start(){};
    virtual  void cancel(){};
    virtual  void release(){};
    virtual  float getProgress(){};
    virtual  int getState(){};
    virtual  ~Task(){};
};
#endif //AUDIOANDVIDEOEDITOR_TASK_H
