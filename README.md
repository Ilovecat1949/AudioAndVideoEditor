# 音视频编辑器

![Image](app/src/main/ic_launcher-playstore.png)  

[![Download](https://img.shields.io/badge/download-App-blue.svg)](https://github.com/Ilovecat1949/AudioAndVideoEditor/raw/refs/heads/master/app/release/app-release.apk)


## 简介
这是一款基于FFmpeg的Android音视频编辑器，目标是在Android上提供大多数FFmpeg的功能。目前支持视频时长剪裁、FFmpeg命令行、视频重编码、获取音视频信息功能。  
本项目目前只是为了跑通FFmpeg+Android的全流程，实现了最基本的解码和编码功能，后面会陆续将FFmpeg支持的音视频编辑功能加上去。  
本项目代码写的比较粗糙，如有不对请多多指教。有什么问题或者建议可以邮箱联系我。  
本项目支持简体中文和英语两种语言，可以在设置里面切换语言。
## 开发和运行环境
本项目由Android Studio开发，使用的编程语言是Kotlin和C++，使用的FFmpeg版本是4.2.9。  
本项目应该支持Android 10及至Android 14的Android系统，在我自己的x86的Android 10虚拟机和arm64-v8a的Android 14的真机跑通过。  
Android 15由于我没有适配 16K Page Size，所以应该是无法在Android 15上运行的。  
本项目应用运行需获取文件读写权限和通知权限。文件读写权限用于读取本机音视频文件和生成新的音视频文件，是必需的权限。  
通知权限主要用于告知用户任务执行进度，是非必需的。
## 基本功能
### 支持视频时长剪裁
剪裁指定开始时间和结束时间的视频片段
### 获取音视频信息
选择音视频文件，可以获取音视频的分辨率、帧率、码率、时长、声道数等信息
### 音视频重编码
选择音视频文件，可以设置编码的帧率、码率等信息，重新编码音视频文件。
### 支持FFmpeg命令行
执行用户输入的FFmpeg命令行
## 参考
1.[Android 音视频开发打怪升级系列文章](https://juejin.cn/post/6844903949451919368)  
2.[LearningVideo](https://github.com/ChenLittlePing/LearningVideo)  
3.[MediaPipe + FFmpeg生成绿幕视频的另一种方式](https://juejin.cn/post/7323398442730078245)  
4.[Jetpack Compose Codelabs Android官方示例项目](https://github.com/android/codelab-android-compose) 
