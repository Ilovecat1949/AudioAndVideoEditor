package com.example.audioandvideoeditor.manager

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.audioandvideoeditor.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 应用广告/推广统一管理器（单例）
 * 当前职责：负责 GitHub Star 弹窗 WebView 的后台预加载与生命周期管理
 * 拓展职责：后续商业化可扩展穿山甲/AdMob等第三方广告 SDK 的预加载与策略调度
 */
class AdManager(private val context: Context) {

    //为录屏任务专门提供的广告标志
//    var showADFlag=false
    // 状态标识：网页是否已加载完成
    private val _isAdLoaded = MutableStateFlow(false)
    val isAdLoaded: StateFlow<Boolean> = _isAdLoaded

    private var webView: WebView? = null

    /**
     * 预加载广告网页
     * 必须传入 ApplicationContext 避免 Activity 泄漏
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun preloadAd() {
        if (webView != null) return

        val appContext = context.applicationContext
        val adUrl = appContext.getString(R.string.link)

        webView = WebView(appContext).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // 优先使用缓存，提速网页渲染
            settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    _isAdLoaded.value = true
                }
            }
            loadUrl(adUrl)
        }
    }

    /**
     * 安全获取 WebView 实例
     * 核心解绑：在交付给 Compose 的 AndroidView 挂载前，先剥离已存在的 Parent 关系，防止崩溃
     */
    fun getWebViewInstance(): WebView? {
        val instance = webView ?: return null
        (instance.parent as? ViewGroup)?.removeView(instance)
        return instance
    }

    /**
     * 释放 WebView 资源（在 App 退出或不再需要时调用）
     */
    fun destroy() {
        webView?.stopLoading()
        (webView?.parent as? ViewGroup)?.removeView(webView)
        webView?.destroy()
        webView = null
        _isAdLoaded.value = false
    }
}