//
// Created by deng on 2024/10/31.
//

#ifndef AUDIOANDVIDEOEDITOR_FFAUDIOFIFO_H
#define AUDIOANDVIDEOEDITOR_FFAUDIOFIFO_H

extern "C" {
#include <libavcodec/avcodec.h>
#include <libswscale/swscale.h>
#include <libavformat/avformat.h>
#include <libavutil/opt.h>
#include <libavutil/frame.h>
#include "libavutil/audio_fifo.h"
};
class FFAudioFifo {
private:
    const  char * TAG="FFAudioFifo";
    AVAudioFifo *fifo = NULL;
    AVCodecContext *codec_context=NULL;
    int init_frame(AVFrame **frame, int frame_size);
    int64_t pts = 0;

public:
    //FFAudioFifo(AVCodecContext *codec_context);
    int init(AVCodecContext *codec_ctx);
    int getSize();
    int add_samples_to_fifo(uint8_t **converted_input_samples,const int frame_size);
    int getAVFrame(AVFrame* &frame);
    ~FFAudioFifo();
};


#endif //AUDIOANDVIDEOEDITOR_FFAUDIOFIFO_H
