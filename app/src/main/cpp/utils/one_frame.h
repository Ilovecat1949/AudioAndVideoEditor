//
// Created by deng on 2024/10/31.
//

#ifndef AUDIOANDVIDEOEDITOR_ONE_FRAME_H
#define AUDIOANDVIDEOEDITOR_ONE_FRAME_H

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/frame.h>
};
class OneFrame {
public:
    AVFrame * frame=NULL;
    AVRational time_base;
//    int64_t i=0;
    uint8_t *buf_for_frame=NULL;
    OneFrame(AVFrame * frame,uint8_t *buf_for_frame, AVRational time_base) {
        this->frame=frame;
        this->buf_for_frame=buf_for_frame;
        this->time_base = time_base;
    }
    ~OneFrame() {
        if(frame!=NULL){
            av_frame_free(&frame);
            frame=NULL;
        }
        if(buf_for_frame!=NULL){
            av_freep(&buf_for_frame);
            buf_for_frame=NULL;
        }
    }
};
#endif //AUDIOANDVIDEOEDITOR_ONE_FRAME_H
