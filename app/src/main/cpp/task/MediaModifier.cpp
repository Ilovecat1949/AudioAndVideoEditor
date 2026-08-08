//
// Created by deng on 2026/7/25.
//

#include "MediaModifier.h"
#include "MediaModifier.h"
#include "../utils/logger.h" // 假设你的日志工具在此路径
#include <sstream>
#include <cstdlib>

std::map<std::string, std::string> MediaModifier::parseCommands(const std::string& editCommands) {
    std::map<std::string, std::string> cmdMap;
    std::stringstream ss(editCommands);
    std::string line;

    while (std::getline(ss, line,'\n')) {
        if (line.empty()) continue;
        auto pos = line.find(':');
        if (pos != std::string::npos) {
            std::string key = line.substr(0, pos);
            std::string value = line.substr(pos + 1);
            cmdMap[key] = value;
        }
    }
    return cmdMap;
}

int MediaModifier::applyFastEdit(const char* inputPath, const char* outputPath, const char* editCommands) {
    if (!inputPath || !outputPath) {
        LOG_ERROR(TAG, "applyFastEdit", "Input or Output path is null");
        return -1;
    }

    std::map<std::string, std::string> cmdMap;
    if (editCommands) {
        cmdMap = parseCommands(editCommands);
    }

    AVFormatContext* in_fmt_ctx = nullptr;
    AVFormatContext* out_fmt_ctx = nullptr;
    int ret = 0;

    // 1. 打开输入文件
    if ((ret = avformat_open_input(&in_fmt_ctx, inputPath, nullptr, nullptr)) < 0) {
        LOG_ERROR(TAG, "avformat_open_input", "Fail to open input file [%s], ret=%d", inputPath, ret);
        return ret;
    }

    // 2. 获取输入流信息
    if ((ret = avformat_find_stream_info(in_fmt_ctx, nullptr)) < 0) {
        LOG_ERROR(TAG, "avformat_find_stream_info", "Fail to find stream info, ret=%d", ret);
        avformat_close_input(&in_fmt_ctx);
        return ret;
    }

    // 3. 创建输出 Context
    std::string format_name;
    if (in_fmt_ctx->iformat && in_fmt_ctx->iformat->name) {
        std::string raw_name = in_fmt_ctx->iformat->name;

        // 🌟 优化1: 针对 mov,mp4,m4a 这种复合格式，优先检索是否有 mp4；无则截取第一个
        if (raw_name.find("mp4") != std::string::npos) {
            format_name = "mp4";
        } else {
            auto pos = raw_name.find(',');
            format_name = (pos != std::string::npos) ? raw_name.substr(0, pos) : raw_name;
        }
    }
    // 🌟 优化2: 特殊 Demuxer 名称矫正（防止某些只有 Demuxer 没有 Muxer 的名字导致报错）
    if (format_name == "live_flv") {
        format_name = "flv";
    }

    const char* target_fmt = format_name.empty() ? nullptr : format_name.c_str();

    // 第一次尝试：用解析出的 format_name 或让 FFmpeg 自动根据 outputPath 后缀猜
    ret = avformat_alloc_output_context2(&out_fmt_ctx, nullptr, target_fmt, outputPath);

    // 🌟 修正关键点：如果第一次失败，必须先释放/置空 out_fmt_ctx，再尝试第二次！
    if (ret < 0 || !out_fmt_ctx) {
        LOG_INFO(TAG, "applyFastEdit", "Failed with format [%s], ret=%d. Safe fallback to 'mp4'",
                 target_fmt ? target_fmt : "null", ret);

        // 安全清理，确保传入第二次时 *out_fmt_ctx 必为 nullptr
        if (out_fmt_ctx) {
            avformat_free_context(out_fmt_ctx);
            out_fmt_ctx = nullptr;
        }

        // 第二次尝试：保底兜底 MP4
        ret = avformat_alloc_output_context2(&out_fmt_ctx, nullptr, "mp4", outputPath);
    }
    // 最终校验
    if (ret < 0 || !out_fmt_ctx) {
        LOG_ERROR(TAG, "avformat_alloc_output_context2", "Fatal: Fail to alloc output context, ret=%d", ret);
        avformat_close_input(&in_fmt_ctx);
        return ret;
    }

    // 4. 复制输入容器中的 Metadata，并应用新的元数据修改
    av_dict_copy(&out_fmt_ctx->metadata, in_fmt_ctx->metadata, 0);
    for (const auto& [key, value] : cmdMap) {
        if (key == "title" || key == "artist" || key == "author" ||
            key == "description" || key == "comment" || key == "copyright"||
            key == "album"||key == "genre"||key == "creation_time"
            ) {
            av_dict_set(&out_fmt_ctx->metadata, key.c_str(), value.c_str(), 0);
        }
    }

    // 5. 遍历流，复制流轨道与参数 (Stream Mapping)
    for (unsigned int i = 0; i < in_fmt_ctx->nb_streams; i++) {
        AVStream* in_stream = in_fmt_ctx->streams[i];
        AVStream* out_stream = avformat_new_stream(out_fmt_ctx, nullptr);
        if (!out_stream) {
            LOG_ERROR(TAG, "avformat_new_stream", "Failed allocating output stream");
            ret = AVERROR_UNKNOWN;
            goto end;
        }

        // 复制 Codec 编码参数 (-c copy)
        ret = avcodec_parameters_copy(out_stream->codecpar, in_stream->codecpar);
        if (ret < 0) {
            LOG_ERROR(TAG, "avcodec_parameters_copy", "Failed to copy codec parameters, ret=%d", ret);
            goto end;
        }
        out_stream->codecpar->codec_tag = 0;

        // 复制流元数据
        av_dict_copy(&out_stream->metadata, in_stream->metadata, 0);

        // 如果是视频流，检查是否有旋转角度 (rotation) 的修改请求
        if (in_stream->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            auto it = cmdMap.find("rotation");
            if (it != cmdMap.end()) {
                av_dict_set(&out_stream->metadata, "rotate", it->second.c_str(), 0);
            }
        }
    }

    // 6. 打开输出文件 IO
    if (!(out_fmt_ctx->oformat->flags & AVFMT_NOFILE)) {
        ret = avio_open(&out_fmt_ctx->pb, outputPath, AVIO_FLAG_WRITE);
        if (ret < 0) {
            LOG_ERROR(TAG, "avio_open", "Could not open output file [%s], ret=%d", outputPath, ret);
            goto end;
        }
    }

    // 7. 写入容器 Header
    ret = avformat_write_header(out_fmt_ctx, nullptr);
    if (ret < 0) {
        LOG_ERROR(TAG, "avformat_write_header", "Error occurred when writing header, ret=%d", ret);
        goto end;
    }

    // 8. 逐帧读取并写入数据包 (Packet Remuxing)
    AVPacket pkt;
    while (1) {
        ret = av_read_frame(in_fmt_ctx, &pkt);
        if (ret < 0) break; // EOF 或读取完毕

        AVStream* in_stream  = in_fmt_ctx->streams[pkt.stream_index];
        AVStream* out_stream = out_fmt_ctx->streams[pkt.stream_index];

        // 重新计算 TimeBase 转换为输出流时间戳
        pkt.pts = av_rescale_q_rnd(pkt.pts, in_stream->time_base, out_stream->time_base,
                                   static_cast<AVRounding>(AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
        pkt.dts = av_rescale_q_rnd(pkt.dts, in_stream->time_base, out_stream->time_base,
                                   static_cast<AVRounding>(AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
        pkt.duration = av_rescale_q(pkt.duration, in_stream->time_base, out_stream->time_base);
        pkt.pos = -1;

        // 写入 Package（不重编码）
        ret = av_interleaved_write_frame(out_fmt_ctx, &pkt);
        av_packet_unref(&pkt);

        if (ret < 0) {
            LOG_ERROR(TAG, "av_interleaved_write_frame", "Error muxing packet, ret=%d", ret);
            goto end;
        }
    }

    // 9. 写入容器 Trailer
    av_write_trailer(out_fmt_ctx);
    ret = 0; // 执行成功

    end:
    // 资源释放与清理
    if (in_fmt_ctx) {
        avformat_close_input(&in_fmt_ctx);
    }
    if (out_fmt_ctx) {
        if (!(out_fmt_ctx->oformat->flags & AVFMT_NOFILE)) {
            avio_closep(&out_fmt_ctx->pb);
        }
        avformat_free_context(out_fmt_ctx);
    }

    return ret;
}