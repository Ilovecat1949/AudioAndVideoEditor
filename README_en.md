# AudioAndVideoEditor

![Image](app/src/main/ic_launcher-playstore.png)  

[![Download](https://img.shields.io/badge/download-App-blue.svg)](https://github.com/Ilovecat1949/AudioAndVideoEditor/releases)



# Introduction
This is an Android audio and video editor built on FFmpeg. It aims to deliver a comprehensive set of audio and video processing capabilities on the Android platform. Current features include FFmpeg command execution, video compression, video format conversion, video trimming, video re-encoding, and media metadata retrieval.

Feel free to send me your feedback and suggestions via email.

This project supports both Simplified Chinese and English, and the language can be switched in Settings.

# Development & Runtime Environment
The project is developed using Android Studio, with Kotlin and C++ as its programming languages. The FFmpeg version used is 4.2.11.

It supports Android 10 through Android 16. It has been tested successfully on an x86_64 Android 16 virtual machine and an arm64-v8a Android 14 physical device.
16KB Page Size is supported.

The application requires file read/write permission and notification permission to run. File read/write permission is mandatory, used to access local media files and export new audio/video files.
Notification permission is optional, mainly to notify users of task progress.

Disabling battery optimization ensures stable background task execution.

# Features
| Feature Name            | Description                                                                                                                                                                        |
|-------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| FFmpeg Command Line     | Run FFmpeg commands directly on mobile devices. Supports batch task submission, as well as creation and management of custom parameters.                                           |
| Screen Recording        | Four audio modes available: no audio, microphone audio, internal audio, mixed microphone + internal audio.                                                                         |
| Video Compression       | Re-encodes and compresses videos based on user-selected compression ratios, with hardware acceleration support.                                                                    |
| Media Information       | Read audio and video metadata (resolution, bitrate, etc.) and modify media metadata.                                                                                               |
| Video Format Conversion | Reconfigure video container format, video codec, audio codec, resolution, video bitrate, frame rate, audio bitrate, audio sample rate, and more. MP3 and H.265 are supported.      |
| Video Trimming          | Extract user-specified video segments. Two modes are provided: fast trimming (high speed, less precise timestamps) and precise trimming (slower speed, accurate cut points).       |
| Regional Video Editing  | Includes frame cropping, watermark removal, watermark overlay, and mosaic effects.                                                                                                 |
| Aspect Ratio Adjustment | Reset video aspect ratio and customize background color.                                                                                                                           |
| Video Speed Control     | Adjust playback speed of videos.                                                                                                                                                   |
| Audio Extraction        | Extract audio tracks from video files.                                                                                                                                             |
| Video Muting            | Remove all audio tracks from videos.                                                                                                                                               |


## Reference
1.[Android 音视频开发打怪升级系列文章](https://juejin.cn/post/6844903949451919368)  
2.[LearningVideo](https://github.com/ChenLittlePing/LearningVideo)  
3.[MediaPipe + FFmpeg生成绿幕视频的另一种方式](https://juejin.cn/post/7323398442730078245)  
4.[Jetpack Compose Codelabs Android官方示例项目](https://github.com/android/codelab-android-compose) 
