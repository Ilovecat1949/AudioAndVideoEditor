//
// Created by deng on 2024/11/4.
//

#include "AudioAndVideoInfo.h"
#include "../utils/logger.h"



#include <sstream>
#include <iomanip>
std::string AudioAndVideoInfo::getStrInfo() {
    std::stringstream ss;

// 🌟 统一获取容器级别的全局总时长（AV_TIME_BASE 为微秒，转换为秒）
    int64_t total_duration = 0;
    if(m_format_ctx){
        if (m_format_ctx->duration != AV_NOPTS_VALUE) {
            total_duration = m_format_ctx->duration / AV_TIME_BASE;
            ss << "duration:" << total_duration<< "\n";
        }
        if (m_format_ctx->iformat && m_format_ctx->iformat->name) {
            ss << "format_name:" << m_format_ctx->iformat->name << "\n";
        }
        if (m_format_ctx->pb) {
            ss << "file_size:" << avio_size(m_format_ctx->pb) << "\n";
        }
        if(m_format_ctx->metadata){
            AVDictionaryEntry *title = av_dict_get(m_format_ctx->metadata, "title", NULL, 0);
            if (title && title->value) ss << "title:" << title->value << "\n";

            AVDictionaryEntry *creation_time = av_dict_get(m_format_ctx->metadata, "creation_time", NULL, 0);
            if (creation_time && creation_time->value) ss << "creation_time:" << creation_time->value << "\n";
            // 🌟 2. [新增] 作者 / 创作者
            AVDictionaryEntry *artist = av_dict_get(m_format_ctx->metadata, "artist", NULL, 0);
            if (!artist) artist = av_dict_get(m_format_ctx->metadata, "author", NULL, 0);
            if (artist && artist->value) ss << "artist:" << artist->value << "\n";

            // 🌟 3. [新增] 文件描述 / 备注 comment才是有效的
            AVDictionaryEntry *desc = av_dict_get(m_format_ctx->metadata, "description", NULL, 0);
            if (!desc) desc = av_dict_get(m_format_ctx->metadata, "comment", NULL, 0);
            if (desc && desc->value) ss << "description:" << desc->value << "\n";

            // 🌟 4. [新增] 版权与编码工具
            AVDictionaryEntry *copyright = av_dict_get(m_format_ctx->metadata, "copyright", NULL, 0);
            if (copyright && copyright->value) ss << "copyright:" << copyright->value << "\n";

            AVDictionaryEntry *encoder = av_dict_get(m_format_ctx->metadata, "encoder", NULL, 0);
            if (encoder && encoder->value) ss << "encoder:" << encoder->value << "\n";

            AVDictionaryEntry *album = av_dict_get(m_format_ctx->metadata, "album", NULL, 0);
            if (album && album->value) ss << "album:" << album->value << "\n";

            AVDictionaryEntry *genre = av_dict_get(m_format_ctx->metadata, "genre", NULL, 0);
            if (genre && genre->value) ss << "genre:" << genre->value << "\n";

        }
    }

    if(m_format_ctx&&video_stream_index!=-1){
        auto* stream = m_format_ctx->streams[video_stream_index];
    // 1. 旋转矩阵校正
        int display_width = stream->codecpar->width;
        int display_height = stream->codecpar->height;

        int rotate = 0;
        AVDictionaryEntry *tag = av_dict_get(stream->metadata, "rotate", NULL, 0);
        if (tag && tag->value) {
            rotate = atoi(tag->value);
        }
        if (rotate == 90 || rotate == 270) {
            int temp = display_width;
            display_width = display_height;
            display_height = temp;
        }

        ss << "width:" << display_width << "\n";
        ss << "height:" << display_height << "\n";
        ss << "rotation:" << rotate << "\n";
        double fps = 0.0;
        // 1. 优先获取平均帧率 (avg_frame_rate)
        if (stream->avg_frame_rate.den != 0 && stream->avg_frame_rate.num != 0) {
            fps = av_q2d(stream->avg_frame_rate);
        }
        // 2. 算不出平均帧率时，回退到最小公共帧率 (r_frame_rate)
        if (fps <= 0.0 && stream->r_frame_rate.den != 0) {
            fps = av_q2d(stream->r_frame_rate);
        }
        // 只要能算出有效数值就如实输出
        if (fps > 0.0) {
            ss << "frame_rate:" << std::fixed << std::setprecision(2) << fps << "\n";
        }
        ss << "video_bit_rate:" << stream->codecpar->bit_rate << "\n";
         // 时长安全防御
        int64_t v_dur = 0;
        if (stream->duration != AV_NOPTS_VALUE) {
            v_dur = static_cast<int64_t>(stream->duration * av_q2d(stream->time_base));
        }
        if (v_dur <= 0) {
            v_dur = total_duration;
        }
        ss << "video_duration:" << v_dur << "\n";
        if (stream->codecpar->format != AV_PIX_FMT_NONE) {
            const char* pix_fmt_name = av_get_pix_fmt_name(static_cast<AVPixelFormat>(stream->codecpar->format));
            if (pix_fmt_name) {
                ss << "pixel_format:" << pix_fmt_name << "\n";
            }
        }
        auto* codecpar = stream->codecpar;

        // 色彩空间与传递特性 (HDR/SDR 识别)
        const char* cs_name = av_color_space_name(codecpar->color_space);
        if (cs_name) ss << "color_space:" << cs_name << "\n";

        const char* trc_name = av_color_transfer_name(codecpar->color_trc);
        if (trc_name) ss << "color_transfer:" << trc_name << "\n";

        // 采样宽高比 (SAR)
        if (codecpar->sample_aspect_ratio.den != 0) {
            ss << "sar_num:" << codecpar->sample_aspect_ratio.num << "\n";
            ss << "sar_den:" << codecpar->sample_aspect_ratio.den << "\n";
        }

        // 视频 Profile
        const char* v_profile = avcodec_profile_name(codecpar->codec_id, codecpar->profile);
        if (v_profile) ss << "video_profile:" << v_profile << "\n";
// 映射视频编码类型
        enum AVCodecID codec_id = stream->codecpar->codec_id;
        std::string codec_name = "UNKNOWN";
        switch (codec_id) {
            case AV_CODEC_ID_H264: codec_name = "H.264(AVC)"; break;
            case AV_CODEC_ID_H265: codec_name = "H.265(HEVC)"; break;
            case AV_CODEC_ID_AV1:  codec_name = "AV1"; break;          // 🌟 补上 AV1
            case AV_CODEC_ID_VP9:  codec_name = "VP9"; break;
            case AV_CODEC_ID_VP8:  codec_name = "VP8"; break;          // 🌟 补上 VP8
            case AV_CODEC_ID_PRORES: codec_name = "Apple ProRes"; break;// 🌟 补上 ProRes
            case AV_CODEC_ID_MPEG4: codec_name = "MPEG-4"; break;
            default: {
                // 🌟 终极兜底：让 FFmpeg 官方描述来提供名字！绝不会出现 UNKNOWN
                const AVCodecDescriptor *desc = avcodec_descriptor_get(codec_id);
                if (desc && desc->name) {
                    codec_name = desc->name; // 例如输出 "mjpegb", "wmv3", "rawvideo" 等
                }
                break;
            }
        }
        ss << "video_codec_type:" << codec_name << "\n";
    }
// 音频流信息收集
    if (m_format_ctx&&audio_stream_index != -1) {
        auto* stream = m_format_ctx->streams[audio_stream_index];
        ss << "sample_rate:" << stream->codecpar->sample_rate << "\n";

        // 兼容现代 FFmpeg 的声道获取方案
        int channels = stream->codecpar->channels;
        ss << "channels:" << channels << "\n";

        // 音频码率安全兜底
        int64_t a_bitrate = stream->codecpar->bit_rate;
        if (a_bitrate <= 0 && m_format_ctx->bit_rate > 0) {
            int64_t v_bitrate = (video_stream_index != -1) ? m_format_ctx->streams[video_stream_index]->codecpar->bit_rate : 0;
            if (m_format_ctx->bit_rate > v_bitrate) {
                a_bitrate = m_format_ctx->bit_rate - v_bitrate;
            }
        }
        ss << "audio_bit_rate:" << a_bitrate << "\n";

        // 音频时长安全防御
        int64_t a_dur = 0;
        if (stream->duration != AV_NOPTS_VALUE) {
            a_dur = static_cast<int64_t>(stream->duration * av_q2d(stream->time_base));
        }
        if (a_dur <= 0) a_dur = total_duration;
        ss << "audio_duration:" << a_dur << "\n";

        auto* codecpar = stream->codecpar;

        // 音频采样格式
        const char* sample_fmt = av_get_sample_fmt_name(static_cast<AVSampleFormat>(codecpar->format));
        if (sample_fmt) ss << "sample_format:" << sample_fmt << "\n";

        // 音频 Profile
        const char* a_profile = avcodec_profile_name(codecpar->codec_id, codecpar->profile);
        if (a_profile) ss << "audio_profile:" << a_profile << "\n";

        // 映射音频编码类型
        enum AVCodecID codec_id = stream->codecpar->codec_id;
        std::string audio_codec = "UNKNOWN";
        switch (codec_id) {
            case AV_CODEC_ID_AAC: audio_codec = "AAC"; break;
            case AV_CODEC_ID_MP3:
            case AV_CODEC_ID_MP3ADU:
            case AV_CODEC_ID_MP3ON4: audio_codec = "MP3"; break;
            case AV_CODEC_ID_FLAC: audio_codec = "FLAC"; break;
            case AV_CODEC_ID_OPUS: audio_codec = "OPUS"; break;
            case AV_CODEC_ID_AC3:  audio_codec = "AC3"; break;
            case AV_CODEC_ID_EAC3: audio_codec = "E-AC-3"; break;      // 🌟 补上 EAC3
            case AV_CODEC_ID_ALAC: audio_codec = "ALAC"; break;        // 🌟 补上 ALAC
            default: {
                // 🌟 识别所有 PCM 格式（WAV 无损录音）
                if (codec_id >= AV_CODEC_ID_FIRST_AUDIO && codec_id < AV_CODEC_ID_MP2) {
                    audio_codec = "PCM";
                } else {
                    // 🌟 终极兜底
                    const AVCodecDescriptor *desc = avcodec_descriptor_get(codec_id);
                    if (desc && desc->name) {
                        audio_codec = desc->name; // 例如输出 "vorbis", "amr_nb", "wmapro" 等
                    }
                }
                break;
            }
        }
        ss << "audio_codec_type:" << audio_codec << "\n";
    }
    if (subtitle_stream_index != -1) {
        ss << "has_subtitle:true\n";
        auto* sub_stream = m_format_ctx->streams[subtitle_stream_index];
        // 获取字幕语言
        AVDictionaryEntry *lang = av_dict_get(sub_stream->metadata, "language", NULL, 0);
        if (lang && lang->value) {
            ss << "subtitle_language:" << lang->value << "\n";
        }
    } else {
        ss << "has_subtitle:false\n";
    }
    return ss.str();
}

int AudioAndVideoInfo::Init(const char *path) {
    if (!path) {
        return -1;
    }
    this->m_path = path;
    //1，初始化上下文
    m_format_ctx = avformat_alloc_context();
    int state=0;
    //2，打开文件
    state=avformat_open_input(&m_format_ctx, m_path.c_str(), NULL, NULL);
    if (state != 0) {
        LOG_ERROR(TAG, "avformat_open_input", "Fail to open file [%s]", m_path.c_str());
        return state;
    }

    //3，获取音视频流信息
    state=avformat_find_stream_info(m_format_ctx, NULL);
    if (state< 0) {
        LOG_ERROR(TAG, "avformat_find_stream_info", "Fail to find stream info");
        return state;
    }
    int vIdx = -1;//存放视频流的索引
    for (int i = 0; i < m_format_ctx->nb_streams; ++i) {
        if (m_format_ctx->streams[i]->codecpar->codec_type == video_type) {
            vIdx = i;
            break;
        }
    }
    if (vIdx == -1) {
        LOG_ERROR(TAG, "video", "Fail to find stream index")
    }
    else {
        video_stream_index = vIdx;
    }

    vIdx = -1;//存放视频流的索引
    for (int i = 0; i < m_format_ctx->nb_streams; ++i) {
        if (m_format_ctx->streams[i]->codecpar->codec_type == audio_type) {
            vIdx = i;
            break;
        }
    }
    if (vIdx == -1) {
        LOG_ERROR(TAG, "audio", "Fail to find stream index")
    }
    else {
        audio_stream_index = vIdx;
    }
    subtitle_stream_index=-1;
    for (int i = 0; i < m_format_ctx->nb_streams; ++i) {
        if (m_format_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_SUBTITLE) {
            subtitle_stream_index = i;
            break;
        }
    }
    return 0;
}

AudioAndVideoInfo::~AudioAndVideoInfo() {
   if(m_format_ctx!=NULL) {
       avformat_close_input(&m_format_ctx);
       avformat_free_context(m_format_ctx);
       m_format_ctx=NULL;
   }
}
