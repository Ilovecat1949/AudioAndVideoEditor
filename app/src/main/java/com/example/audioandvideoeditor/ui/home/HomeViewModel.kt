package com.example.audioandvideoeditor.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class HomeViewModel: ViewModel() {
    // 你页面正在用的所有字段，原样保留，不加任何权限限制
    var path_or_uri by mutableStateOf("")
//    var nextDestination: () -> Unit by mutableStateOf({})
    // 2. 用一个纯文本路由标志，替代原本的 nextDestination 闭包
    // 记录文件选择完毕后，下一步到底该去哪个剪辑页面
    var target_route by mutableStateOf("")
    var route_flag by mutableStateOf(false)

    var show_on_screen_ad by mutableStateOf(true)
    var show_interstistial_ad by mutableStateOf(false)
    var show_crash_message_flag by mutableStateOf(false)
    var showUpdateDialogFlag by mutableStateOf(true)
}