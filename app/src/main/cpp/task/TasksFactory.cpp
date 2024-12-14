//
// Created by deng on 2024/11/1.
//

#include "TasksFactory.h"
#include "ReEncodingTask.h"

int TasksFactory::createTask(TaskInfo *info) {
    int state=-1;
    int task_type=info->int_arr[0];
    int64_t task_num=info->int64_arr[0];
    Task * new_task=NULL;
    if(task_type==0){
        new_task= new ReEncodingTask();
    }
    if(new_task!=NULL){
        new_task->setInfo(info);
        state=new_task->taskInit();
        this->taskMap[task_num]=new_task;
    }
    return state;
}
void TasksFactory::start(int64_t task_num) {
    if(taskMap.find(task_num)!=taskMap.end()) {
        taskMap[task_num]->start();
    }
}
void TasksFactory::cancel(int64_t task_num) {
    if(taskMap.find(task_num)!=taskMap.end()) {
        taskMap[task_num]->cancel();
    }
}
float TasksFactory::getProgress(int64_t task_num) {
    if(taskMap.find(task_num)!=taskMap.end()) {
        return taskMap[task_num]->getProgress();
    }
    else{
        return 0;
    }
}
int TasksFactory::getState(int64_t task_num) {
    if(taskMap.find(task_num)!=taskMap.end()){
        return taskMap[task_num]->getState();
    }
    else{
        return -1;
    }
}
void TasksFactory::release(int64_t task_num) {
    if(taskMap.find(task_num)!=taskMap.end()){
        delete taskMap[task_num];
        taskMap.erase(task_num);
    }

}
