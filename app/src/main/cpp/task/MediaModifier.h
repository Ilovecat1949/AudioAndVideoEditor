//
// Created by deng on 2026/7/25.
//

#ifndef AUDIOANDVIDEOEDITOR_MEDIAMODIFIER_H
#define AUDIOANDVIDEOEDITOR_MEDIAMODIFIER_H
#include <string>
#include <map>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/dict.h>
#include <libavutil/opt.h>
};

class MediaModifier {
private:
    const char* TAG = "MediaModifier";

    // 解析 "key1=val1\nkey2=val2" 的指令字符串
    std::map<std::string, std::string> parseCommands(const std::string& editCommands);

public:
    MediaModifier() = default;
    ~MediaModifier() = default;

    /**
     * 快速修改元数据/流属性（Remux 模式）
     * @param inputPath  输入原文件路径
     * @param outputPath 输出临时文件路径
     * @param editCommands 修改指令（如 "title=新标题\nartist=创作者\nrotation=90"）
     * @return 0 表示成功，负数表示失败错误码
     */
    int applyFastEdit(const char* inputPath, const char* outputPath, const char* editCommands);
};


#endif //AUDIOANDVIDEOEDITOR_MEDIAMODIFIER_H
