package com.example.audioandvideoeditor.components


import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.audioandvideoeditor.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoodsExpressScreen(
    mainUrl: String,
    onClose: () -> Unit
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // 加载状态与进度管理
    var isLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }

    // 🌟 错误与超时状态记录
    var isErrorOccurred by remember { mutableStateOf(false) }

    // 超时定时器（默认 10 秒超时）
    val timeoutHandler = remember { Handler(Looper.getMainLooper()) }
    val timeoutRunnable = remember {
        Runnable {
            if (isLoading) {
                webViewInstance?.stopLoading()
                isLoading = false
                isErrorOccurred = true
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            timeoutHandler.removeCallbacks(timeoutRunnable)
        }
    }

    // 🌟 智能安全后退：跳过加载失败的历史页面
    val handleSmartBack = {
        val webView = webViewInstance
        if (webView != null) {
            val historyList = webView.copyBackForwardList()
            val currentIndex = historyList.currentIndex

            // 向上查找最近一个非错误的正常历史记录
            var targetIndex = -1
            for (i in currentIndex - 1 downTo 0) {
                val item = historyList.getItemAtIndex(i)
                val url = item.url
                if (!url.startsWith("data:text/html") && url != "about:blank") {
                    targetIndex = i
                    break
                }
            }

            if (targetIndex != -1) {
                val steps = targetIndex - currentIndex
                webView.goBackOrForward(steps)
                isErrorOccurred = false
            } else if (canGoBack && webView.url != mainUrl) {
                webView.goBack()
            } else {
                onClose()
            }
        } else {
            onClose()
        }
    }

    // 拦截系统物理/手势返回键
    BackHandler(enabled = true) {
        handleSmartBack()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.exit_goods_express),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { handleSmartBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // 顶栏下方加载进度条
                if (isLoading && loadingProgress < 1.0f) {
                    LinearProgressIndicator(
                        progress = { loadingProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
            }
        },
        bottomBar = {
            GoodsExpressBottomBar(
                canGoBack = canGoBack || isErrorOccurred,
                canGoForward = canGoForward,
                onBackClick = { handleSmartBack() },
                onForwardClick = { if (canGoForward) webViewInstance?.goForward() },
                onRefreshClick = {
                    isErrorOccurred = false
                    webViewInstance?.reload()
                },
                onCloseClick = onClose
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            AdWebView(
                modifier = Modifier.fillMaxSize(),
                url = mainUrl,
                onCreated = { webView ->
                    webViewInstance = webView

                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            isErrorOccurred = false
                            loadingProgress = 0.1f // 🌟 立即给用户 10% 进度反馈，消除“点击无反应”困惑

                            // 启动 10 秒超时定时器
                            timeoutHandler.removeCallbacks(timeoutRunnable)
                            timeoutHandler.postDelayed(timeoutRunnable, 10000)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            timeoutHandler.removeCallbacks(timeoutRunnable)
                            isLoading = false
                            canGoBack = view?.canGoBack() == true
                            canGoForward = view?.canGoForward() == true
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            // 仅主框架加载失败时标记错误
                            if (request?.isForMainFrame == true) {
                                timeoutHandler.removeCallbacks(timeoutRunnable)
                                isLoading = false
                                isErrorOccurred = true
                            }
                        }
                    }

                    webView.webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            loadingProgress = (newProgress / 100f).coerceAtLeast(0.1f)
                            if (newProgress >= 100) {
                                timeoutHandler.removeCallbacks(timeoutRunnable)
                                isLoading = false
                            }
                        }
                    }
                }
            )

            // 中央居中加载菊花图
            if (isLoading && loadingProgress < 0.3f) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

/**
 * 底部控制栏：后退、前进、刷新、退出
 */
@Composable
private fun GoodsExpressBottomBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 后退按钮
            IconButton(
                onClick = onBackClick,
                enabled = canGoBack
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Page Back",
                    tint = if (canGoBack) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            // 前进按钮
            IconButton(
                onClick = onForwardClick,
                enabled = canGoForward
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Page Forward",
                    tint = if (canGoForward) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            // 刷新按钮
            IconButton(onClick = onRefreshClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // 一键退出按钮
            IconButton(onClick = onCloseClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit Station",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}