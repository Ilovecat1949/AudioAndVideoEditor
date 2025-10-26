package com.example.audioandvideoeditor.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audioandvideoeditor.utils.AdContent
//import com.google.firebase.analytics.FirebaseAnalytics
//import com.google.firebase.remoteconfig.FirebaseRemoteConfig
//import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import android.app.Application
// 封装单条广告信息的数据类


// 封装广告弹窗的所有数据
data class AdUiState(
    val showDialog: Boolean = false,
    val ads: List<AdContent> = emptyList()
)
class AdViewModel : ViewModel() {

//    private lateinit var analytics: FirebaseAnalytics
//    private lateinit var remoteConfig: FirebaseRemoteConfig
    private val _adUiState = MutableStateFlow(AdUiState())
    val adUiState: StateFlow<AdUiState> = _adUiState
    var init_flag=false
//    fun initFirebase(context: Context){
//        analytics=FirebaseAnalytics.getInstance(context)
//        remoteConfig = FirebaseRemoteConfig.getInstance()
//        fetchRemoteConfig()
//        init_flag=true
//        Log.d("fetchRemoteConfig","adsList.size:${_adUiState.value.ads.size}")
//    }
//    init {
//        fetchRemoteConfig()
//    }

//    private fun fetchRemoteConfig() {
//        // 设置默认值，防止网络问题
//        remoteConfig.setDefaultsAsync(mapOf(
//            "custom_ads_json" to "[]" // 默认不显示广告，空列表
//        ))
//
//        // 配置获取间隔，方便开发调试
//        val configSettings = FirebaseRemoteConfigSettings.Builder()
//            .setMinimumFetchIntervalInSeconds(0)
//            .setFetchTimeoutInSeconds(5)
//            .build()
//        remoteConfig.setConfigSettingsAsync(configSettings)
//
//        // 异步获取并激活远程配置
//        remoteConfig.fetchAndActivate()
//            .addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    val adsJsonString = remoteConfig.getString("custom_ads_json")
//                    val adsList = try {
//                        // 使用 kotlinx.serialization 解析 JSON 字符串
//                        Json.decodeFromString<List<AdContent>>(adsJsonString)
//                    } catch (e: Exception) {
//                        Log.d("fetchRemoteConfig","Failed to parse ad config JSON: $e")
//                        emptyList()
//                    }
//                    Log.d("fetchRemoteConfig","adsList.size:${adsList.size}")
//                    _adUiState.value = _adUiState.value.copy(ads = adsList)
//                }
//                else {
//                    task.exception?.let { exception ->
//                        // 打印具体的异常信息
//                        Log.d("fetchRemoteConfig","Remote Config fetch failed with exception: ${exception.message}")
//                    }
//                }
//                Log.d("fetchRemoteConfig","hhhh")
//            }
//        Log.d("fetchRemoteConfig","adsList.size")
//    }

//    fun onTaskStarted() {
//        // 在任务开始时，如果广告列表不为空，则显示弹窗
//        if (_adUiState.value.ads.isNotEmpty()) {
//            _adUiState.value = _adUiState.value.copy(showDialog = true)
//            // 记录广告曝光事件
//            _adUiState.value.ads.forEachIndexed { index, ad ->
//                logAdImpression(ad, index)
//            }
//        }
//    }

    fun onAdDismissed() {
        // 弹窗关闭后，隐藏广告
        _adUiState.value = _adUiState.value.copy(showDialog = false)
    }

//    fun onAdClicked(ad: AdContent, adIndex: Int) {
//        // 记录广告点击事件
//        logAdClick(ad, adIndex)
//    }

    // --- Firebase Analytics 事件日志函数 ---

//    private fun logAdImpression(ad: AdContent, adIndex: Int) {
//        val params = Bundle().apply {
//            putString("ad_title", ad.title)
//            putString("ad_image_url", ad.imageUrl)
//            putInt("ad_index", adIndex)
//        }
//        analytics.logEvent("ad_impression", params)
//    }
//
//    private fun logAdClick(ad: AdContent, adIndex: Int) {
//        val params = Bundle().apply {
//            putString("ad_title", ad.title)
//            putString("ad_click_url", ad.clickUrl)
//            putInt("ad_index", adIndex)
//        }
//        analytics.logEvent("ad_click", params)
//    }
    private val _isAdLoaded = MutableStateFlow(false)
    val isAdLoaded: StateFlow<Boolean> = _isAdLoaded

    private var webView: WebView? = null
    // 使用 applicationContext，避免任何潜在的 Context 泄漏
    @SuppressLint("SetJavaScriptEnabled")
    fun preloadAd(adUrl: String,context: Context) {
        if (webView != null || _isAdLoaded.value) {
            return
        }

        viewModelScope.launch(Dispatchers.Main) {
            webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        _isAdLoaded.value = true
                        println("Ad page preloaded successfully!")
                    }
                }
            }
            webView?.loadUrl(adUrl)
        }
    }

    fun getWebViewInstance(): WebView? {
        val instance = webView
//        webView = null
        _isAdLoaded.value = false
        return instance
    }

    override fun onCleared() {
        super.onCleared()
        webView?.stopLoading()
        webView?.destroy()
        webView = null
        println("AdViewModel has been cleared, WebView destroyed.")
    }
}