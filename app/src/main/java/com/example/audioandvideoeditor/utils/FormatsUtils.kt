package com.example.audioandvideoeditor.utils

object FormatsUtils {
    val FullySupportsFormatsConfigs=mapOf(
        "MP4" to TargetFormatSupport(
            videoCodecs = setOf("H.265(HEVC)","H.264(AVC)", "MPEG-4 Part 2","AV1","MPEG-2","MPEG-1","VP9","VP8","VC-1","H.263","M-JPEG","MJ2"),
            audioCodecs = setOf("AAC","MP3","AC-3","E-AC-3","DTS","OPUS","MP2","MP1","FLAC","ALAC","DTS-HD","Dolby TrueHD","MLP","ALS","SLS","LPCM",
                "DV Audio","AMR")
        ),
        "AVI" to TargetFormatSupport(
            videoCodecs = setOf("H.265(HEVC)","H.264(AVC)","VP9","VP8","MPEG-2","MPEG-1","MPEG-4 Part 2","WMV","VC-1","H.263","Cinepak","DV","M-JPEG","YCbCr"),
            audioCodecs = setOf("MP3","AC-3","DTS","WMA","OPUS","MP2","MP1",
                "FLAC","ALAC","WMA Lossless","LPCM","A-law PCM","μ-law PCM","IEEE floating-point PCM","Microsoft ADPCM","AMR","G.728")
        ),
        "FLV" to TargetFormatSupport(videoCodecs = setOf("H.264(AVC)"), audioCodecs = setOf("MP3", "AAC")),
        "TS" to TargetFormatSupport(videoCodecs = setOf("H.265(HEVC)","H.264(AVC)","MPEG-2","MPEG-1","MPEG-4 Part 2")
            , audioCodecs = setOf("MP3","OPUS","MP2","MP1","ALS","SLS")),
        "MKV" to TargetFormatSupport(videoCodecs = setOf("H.265(HEVC)","H.264(AVC)","AV1","VP9","VP8","MVC","MPEG-2","MPEG-1","MPEG-4 Part 2","WMV","Theora","Cinepak","Sorenson","YCbCr")
            , audioCodecs = setOf("AAC","MP3","AC-3","DTS","OPUS","Vorbis","MP2","MP1","ATRAC3","FLAC","ALAC",
                "DTS-HD","LPCM","IEEE floating-point PCM")),
        "MOV" to TargetFormatSupport(videoCodecs = setOf("H.265(HEVC)","H.264(AVC)","MPEG-2","MPEG-1","VC-1","H.263","Cinepak","M-JPEG","YCbCr","Apple ProRes","Dirac"),
            audioCodecs = setOf("AAC","AC-3","E-AC-3","OPUS","QDesign Music 1 and 2","ALAC",
                "DTS-HD","LPCM","A-law PCM","μ-law PCM","IEEE floating-point PCM",
                "Microsoft ADPCM","DV Audio","QCELP")),
        "MPEG" to TargetFormatSupport(videoCodecs = setOf("MPEG-1", "MPEG-2"), audioCodecs = setOf("MP3", "AC3")),
        "3GP"  to TargetFormatSupport(
            videoCodecs = setOf("H.265(HEVC)","H.264(AVC)",
                "MPEG-4 Part 2","H.263","MVC"),
            audioCodecs = setOf("AMR")
        )
    )
}
data class TargetFormatSupport(
    val videoCodecs: Set<String>,
    val audioCodecs: Set<String>
)