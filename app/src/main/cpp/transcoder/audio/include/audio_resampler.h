#ifndef AUDIO_RESAMPLER_H
#define AUDIO_RESAMPLER_H

#include "base_audio_filter.h"

extern "C" {
#include <libswresample/swresample.h>
#include <libavutil/opt.h>
#include <libavutil/channel_layout.h>
#include <libavutil/samplefmt.h>
}

class AudioResampler : public BaseAudioFilter {
public:
    AudioResampler();
    ~AudioResampler() override;

    bool init(int inSampleRate, int inChannels, int inSampleFmt,
              int outSampleRate, int outChannels, int outSampleFmt) override;
    bool process(const uint8_t* inData, int inSize, uint8_t** outData, int* outSize) override;
    bool flush(uint8_t** outData, int* outSize) override;
    void release() override;

private:
    SwrContext* swrCtx = nullptr;

    int inSampleRate = 0;
    int inChannels = 0;
    AVSampleFormat inSampleFmt = AV_SAMPLE_FMT_S16;
    int outSampleRate = 0;
    int outChannels = 0;
    AVSampleFormat outSampleFmt = AV_SAMPLE_FMT_S16;

    uint8_t* outBuffer = nullptr;
    int outBufferCapacity = 0;
};

#endif // AUDIO_RESAMPLER_H