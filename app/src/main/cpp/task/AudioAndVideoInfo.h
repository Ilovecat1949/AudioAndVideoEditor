//
// Created by deng on 2024/11/4.
//

#ifndef AUDIOANDVIDEOEDITOR_AUDIOANDVIDEOINFO_H
#define AUDIOANDVIDEOEDITOR_AUDIOANDVIDEOINFO_H

#include <string>
#include <stddef.h>
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/frame.h>
#include <libavutil/time.h>
#include "libavutil/pixdesc.h"
};
class AudioAndVideoInfo {
private:
    const char * TAG="AudioAndVideoInfo";
    AVFormatContext *m_format_ctx = NULL;
    int video_stream_index = -1;
    int audio_stream_index = -1;
    int subtitle_stream_index = -1;
    AVMediaType video_type=AVMEDIA_TYPE_VIDEO;
    AVMediaType audio_type=AVMEDIA_TYPE_AUDIO;

    std::string m_path ;
public:
    std::string getStrInfo();
    int Init(const char * path );
    ~AudioAndVideoInfo();
};


#endif //AUDIOANDVIDEOEDITOR_AUDIOANDVIDEOINFO_H
