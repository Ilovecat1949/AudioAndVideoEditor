//
// Created by deng on 2024/11/16.
//

#ifndef AUDIOANDVIDEOEDITOR_FFMPEGINFO_H
#define AUDIOANDVIDEOEDITOR_FFMPEGINFO_H


class FFmpegInfo {
private:
    const char * TAG="FFmpegInfo";
    char * getStrCodecInfo();
public:
    char * getStrFFmpegInfo(int info_type);
};


#endif //AUDIOANDVIDEOEDITOR_FFMPEGINFO_H
