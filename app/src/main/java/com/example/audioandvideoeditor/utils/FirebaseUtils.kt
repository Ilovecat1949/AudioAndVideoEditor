package com.example.audioandvideoeditor.utils

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AdContent(
    val title: String,
    val imageUrl: String,
    val clickUrl: String,
    val description: String
)
object FirebaseUtils {
    private val TAG="FirebaseUtils"
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var remoteConfig: FirebaseRemoteConfig
    private val _adContentList = mutableListOf<AdContent>()
    val adContentList: List<AdContent> get()= _adContentList
    var init_flag=false
    private set
    fun initFirebase(context: Context){
        analytics=FirebaseAnalytics.getInstance(context)
        remoteConfig = FirebaseRemoteConfig.getInstance()
        fetchRemoteConfig()
        init_flag=true
    }
    private fun fetchRemoteConfig() {
        // 设置默认值，防止网络问题
        remoteConfig.setDefaultsAsync(mapOf(
            "custom_ads_json" to "[]" // 默认不显示广告，空列表
        ))

        // 配置获取间隔，方便开发调试
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
            .setFetchTimeoutInSeconds(5)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        // 异步获取并激活远程配置
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val adsJsonString = remoteConfig.getString("custom_ads_json")
                    val adsList = try {
                        // 使用 kotlinx.serialization 解析 JSON 字符串
                        Json.decodeFromString<List<AdContent>>(adsJsonString)
                    } catch (e: Exception) {
                        Log.d(TAG,"Failed to parse ad config JSON: $e")
                        emptyList()
                    }
                    //Log.d("fetchRemoteConfig","adsList.size:${adsList.size}")
                    //_adUiState.value = _adUiState.value.copy(ads = adsList)
                    _adContentList.clear()
                    _adContentList.addAll(0,adsList)
                    Log.d(TAG,"ad size:${adsList.size}")
                }
                else {
                    task.exception?.let { exception ->
                        // 打印具体的异常信息
                        Log.e(TAG,"Remote Config fetch failed with exception: ${exception.message}")
                    }
                }

            }
    }
    fun reFreshRemoteConfig(){
        remoteConfig.fetchAndActivate()
    }
    fun onAdClicked(ad: AdContent, adIndex: Int) {
        // 记录广告点击事件
        logAdClick(ad, adIndex)
    }

    // --- Firebase Analytics 事件日志函数 ---

    fun logAdImpression(ad: AdContent, adIndex: Int) {
        val params = Bundle().apply {
//            putString("ad_title", ad.title)
//            putString("ad_image_url", ad.imageUrl)
            putInt("ad_index", adIndex)
        }
        analytics.logEvent("ad_impression", params)
    }

    private fun logAdClick(ad: AdContent, adIndex: Int) {
        val params = Bundle().apply {
//            putString("ad_title", ad.title)
//            putString("ad_click_url", ad.clickUrl)
            putInt("ad_index", adIndex)
        }
        analytics.logEvent("custom_ad_click", params)
    }
}