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
     */
    virtual bool init(int inSampleRate, int inChannels, int outSampleRate, int outChannels) = 0;

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