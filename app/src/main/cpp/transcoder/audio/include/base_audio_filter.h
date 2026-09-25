#ifndef BASE_AUDIO_FILTER_H
#define BASE_AUDIO_FILTER_H

#include <cstdint>

/**
 * C++ 音频滤镜统一抽象基类
 */
class BaseAudioFilter {
public:
    virtual ~BaseAudioFilter() = default;

/**
     * 初始化滤镜
     * @param inSampleRate  输入采样率
     * @param inChannels    输入声道数
     * @param inSampleFmt   输入采样格式 (对应 FFmpeg AVSampleFormat 枚举)
     * @param outSampleRate 输出采样率
     * @param outChannels   输出声道数
     * @param outSampleFmt  输出采样格式 (对应 FFmpeg AVSampleFormat 枚举)
     */
    virtual bool init(int inSampleRate, int inChannels, int inSampleFmt,
                      int outSampleRate, int outChannels, int outSampleFmt) = 0;

    /**
     * 处理 PCM 数据
     * @param inData 输入数据指针
     * @param inSize 输入数据字节数
     * @param outData 输出数据指针 (内部内存管理)
     * @param outSize 输出有效数据字节数
     */
    virtual bool process(const uint8_t* inData, int inSize, uint8_t** outData, int* outSize) = 0;

    /**
     * 刷空内部残留 Buffer (EOS 阶段)
     */
    virtual bool flush(uint8_t** outData, int* outSize) {
        if (outSize) *outSize = 0;
        return true;
    }

    /**
     * 释放资源
     */
    virtual void release() = 0;
};

#endif // BASE_AUDIO_FILTER_H