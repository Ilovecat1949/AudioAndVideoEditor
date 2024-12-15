# AudioAndVideoEditor
## Introduction
This is an Android audio and video editor based on FFmpeg, with the goal of providing most of the FFmpeg features on Android.   
At present, it supports video re encoding and the function of obtaining audio and video information.  
At present, this project is only aimed at running the entire process of FFmpeg+Android,   
achieving the most basic decoding and encoding functions.   
In the future, FFmpeg will gradually add audio and video editing functions supported by FFmpeg.
The code for this project is written quite crudely. If there are any mistakes,  
please feel free to let me know. If you have any questions or suggestions, please feel free to contact me via email.  
This project supports both Simplified Chinese and English languages, which can be switched in the settings.
## Development and operational environment
This project is developed by Android Studio, using Kotlin and C++programming languages, and FFmpeg version 4.2.9.
This project should support Android systems from Android 10 to Android 14,   
and run on my own x86 Android 10 virtual machine and arm64-v8a Android 14 real machine.
Android 15 should not run on Android 15 because I am not compatible with 16K Page Size.
## Basic functions
### Obtain audio and video information
By selecting audio and video files, you can obtain information such as resolution, frame rate, bitrate, duration, and number of channels for the audio and video
### Audio and video re encoding
Select audio and video files to set encoding frame rates, bitrate, and other information, and re encode the audio and video files.
## Reference
1.[Android 音视频开发打怪升级系列文章](https://juejin.cn/post/6844903949451919368)  
2.[LearningVideo](https://github.com/ChenLittlePing/LearningVideo)  
3.[MediaPipe + FFmpeg生成绿幕视频的另一种方式](https://juejin.cn/post/7323398442730078245)  
4.[Jetpack Compose Codelabs Android官方示例项目](https://github.com/android/codelab-android-compose) 