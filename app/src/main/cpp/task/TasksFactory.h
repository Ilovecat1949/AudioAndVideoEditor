//
// Created by deng on 2024/11/1.
//

#ifndef AUDIOANDVIDEOEDITOR_TASKSFACTORY_H
#define AUDIOANDVIDEOEDITOR_TASKSFACTORY_H


#include <stdint.h>
#include "task_info.h"
#include "task.h"
#include <map>
class TasksFactory {
public:
    int createTask(TaskInfo* info);
    void start(int64_t task_num);
    void cancel(int64_t task_num);
    float getProgress(int64_t task_num);
    int getState(int64_t task_num);
    void release(int64_t task_num);
private:
    std::map<int64_t,Task*> taskMap;

};


#endif //AUDIOANDVIDEOEDITOR_TASKSFACTORY_H
