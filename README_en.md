# AudioAndVideoEditor

![Image](app/src/main/ic_launcher-playstore.png)  

[![Download](https://img.shields.io/badge/download-App-blue.svg)](https://github.com/Ilovecat1949/AudioAndVideoEditor/releases)



## Introduction
This is an Android audio and video editor based on FFmpeg, with the goal of providing most of the FFmpeg features on Android.   
At present, it supports functions such as FFmpeg command line, video compression, 
video format conversion, video duration clipping, video re encoding, and obtaining audio and video information.      
The code for this project is written quite crudely. If there are any mistakes,  
please feel free to let me know. If you have any questions or suggestions, please feel free to contact me via email.  
This project supports both Simplified Chinese and English languages, which can be switched in the settings.
## Development and operational environment
This project is developed by Android Studio, using Kotlin and C++programming languages, and FFmpeg version 4.2.9.
This project should support Android systems from Android 10 to Android 14,   
and run on my own x86 Android 10 virtual machine and arm64-v8a Android 14 real machine.
Android 15 should not run on Android 15 because I am not compatible with 16K Page Size.
The application of this project requires obtaining file read and write permissions as well as notification permissions.     
File read and write permissions are necessary for reading local audio and video files and generating new ones.     
Notification permission is mainly used to inform users of the progress of task execution and is not necessary.    
Added prompts and buttons to remove power-saving restrictions to ensure smooth operation of tasks in the background, but it should be noted that this is a potentially dangerous permission.
## Functions
| Function Name                     | Function Details|
|-----------------------------------|--------------------------------------------------------------------------------|  
| ffmpeg command line               | Execute ffmpeg command line on mobile phone|
| Video compression function        | Re encode and compress videos according to the compression ratio selected by the user|
| Video format conversion function  | Used to reset video packaging type, video encoding type, audio encoding type, video resolution, video bitrate, video frame rate, audio bitrate, and audio sampling rate. Supports MP3 and H.265                         |
| Video duration clipping           | Used to clip user specified video clips, with two modes: fast clipping and precise clipping. The execution speed of quick cutting is fast, and the timing of the cut may not be so accurate. The execution speed of precise cutting is slower, and the captured time points will be more accurate.  |
| Video cropping                    | Used to crop user specified video frames.                                                                  |
| Adjust Video Scale                | Used to reset the video scale and background color.                                                              |
| Video speed change                | Used to set the video playback speed.                                                                     |
| Extract Audio                     | Used to extract audio from videos.                                                                     |
| Video Mute                        | Used to eliminate the audio part in a video.                                                                   |

## Reference
1.[Android 音视频开发打怪升级系列文章](https://juejin.cn/post/6844903949451919368)  
2.[LearningVideo](https://github.com/ChenLittlePing/LearningVideo)  
3.[MediaPipe + FFmpeg生成绿幕视频的另一种方式](https://juejin.cn/post/7323398442730078245)  
4.[Jetpack Compose Codelabs Android官方示例项目](https://github.com/android/codelab-android-compose) 
