
#include "include/audio_resampler.h"
#include <android/log.h>
#include <cstdlib>

#define LOG_TAG "AudioResampler"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

AudioResampler::AudioResampler() {}

AudioResampler::~AudioResampler() {
    release();
}

bool AudioResampler::init(int inSampleRate, int inChannels, int inSampleFmt,
                          int outSampleRate, int outChannels, int outSampleFmt) {
    this->inSampleRate = inSampleRate;
    this->inChannels = inChannels;
    this->inSampleFmt = static_cast<AVSampleFormat>(inSampleFmt);

    this->outSampleRate = outSampleRate;
    this->outChannels = outChannels;
    this->outSampleFmt = static_cast<AVSampleFormat>(outSampleFmt);

    // FFmpeg 4.x: 获取 64 位无符号整数表示的声道布局
    int64_t inLayout = av_get_default_channel_layout(inChannels);
    int64_t outLayout = av_get_default_channel_layout(outChannels);

    // 动态传入输入的 inSampleFmt 与输出的 outSampleFmt
    swrCtx = swr_alloc_set_opts(
            nullptr,
            outLayout, this->outSampleFmt, outSampleRate,
            inLayout,  this->inSampleFmt,  inSampleRate,
            0, nullptr
    );

    if (!swrCtx) {
        LOGE("Failed to allocate SwrContext");
        return false;
    }

    if (swr_init(swrCtx) < 0) {
        LOGE("Failed to initialize SwrContext");
        swr_free(&swrCtx);
        return false;
    }

    return true;
}

bool AudioResampler::process(const uint8_t* inData, int inSize, uint8_t** outData, int* outSize) {
    if (!swrCtx || !inData || inSize <= 0) {
        if (outSize) *outSize = 0;
        return false;
    }

    int safeInChannels = inChannels > 0 ? inChannels : 1;
    int safeOutChannels = outChannels > 0 ? outChannels : 2;

    // 动态获取输入和输出每个 Sample 占用的字节数 (例如 S16=2字节, FLTP=4字节)
    int inBytesPerSample = av_get_bytes_per_sample(inSampleFmt);
    int outBytesPerSample = av_get_bytes_per_sample(outSampleFmt);

    if (inBytesPerSample <= 0 || outBytesPerSample <= 0) {
        LOGE("Invalid sample format bytes size!");
        if (outSize) *outSize = 0;
        return false;
    }

    // 精确计算输入的 Sample 数量
    int inSamples = inSize / (safeInChannels * inBytesPerSample);

    // 预估最大输出 Sample 数量 (加上 swr 内部 delay 延迟采样)
    int maxOutSamples = av_rescale_rnd(
            swr_get_delay(swrCtx, inSampleRate) + inSamples,
            outSampleRate, inSampleRate, AV_ROUND_UP
    );

    int requiredSizeBytes = maxOutSamples * safeOutChannels * outBytesPerSample;

    // 动态按需扩容 Buffer
    if (requiredSizeBytes > outBufferCapacity) {
        outBuffer = static_cast<uint8_t*>(realloc(outBuffer, requiredSizeBytes));
        outBufferCapacity = requiredSizeBytes;
    }

    uint8_t* outDataArray[1] = { outBuffer };
    const uint8_t* inDataArray[1] = { inData };

    int outSamples = swr_convert(swrCtx, outDataArray, maxOutSamples, inDataArray, inSamples);
    LOGE("AudioResampler In: inSize=%d, inChannels=%d, inBytesPerSample=%d => inSamples=%d",
         inSize, safeInChannels, inBytesPerSample, inSamples);
    LOGE("AudioResampler Out: outSamples=%d, outSize=%d",
         outSamples, *outSize);
    if (outSamples < 0) {
        LOGE("swr_convert failed!");
        if (outSize) *outSize = 0;
        return false;
    }

    *outData = outBuffer;
    *outSize = outSamples * safeOutChannels * outBytesPerSample;
    return true;
}

bool AudioResampler::flush(uint8_t** outData, int* outSize) {
    if (!swrCtx) {
        if (outSize) *outSize = 0;
        return false;
    }

    int delaySamples = swr_get_delay(swrCtx, inSampleRate);
    if (delaySamples <= 0) {
        if (outSize) *outSize = 0;
        return true;
    }

    int maxOutSamples = av_rescale_rnd(delaySamples, outSampleRate, inSampleRate, AV_ROUND_UP);
    int requiredSizeBytes = maxOutSamples * outChannels * 2;

    if (requiredSizeBytes > outBufferCapacity) {
        outBuffer = static_cast<uint8_t*>(realloc(outBuffer, requiredSizeBytes));
        outBufferCapacity = requiredSizeBytes;
    }

    uint8_t* outDataArray[1] = { outBuffer };

    // 传入 nullptr 刷空内部的残留采样数据
    int outSamples = swr_convert(swrCtx, outDataArray, maxOutSamples, nullptr, 0);
    if (outSamples < 0) {
        if (outSize) *outSize = 0;
        return false;
    }

    *outData = outBuffer;
    *outSize = outSamples * outChannels * 2;
    return true;
}

void AudioResampler::release() {
    if (swrCtx) {
        swr_free(&swrCtx);
        swrCtx = nullptr;
    }
    if (outBuffer) {
        free(outBuffer);
        outBuffer = nullptr;
        outBufferCapacity = 0;
    }
}