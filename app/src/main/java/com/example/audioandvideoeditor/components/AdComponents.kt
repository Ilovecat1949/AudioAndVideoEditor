package com.example.audioandvideoeditor.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.application.AppApplication

import com.example.audioandvideoeditor.utils.FilesUtils
import com.example.audioandvideoeditor.utils.ImageState
import com.example.audioandvideoeditor.utils.observeIgnoringBatteryPermissionStatus
import com.example.audioandvideoeditor.viewmodel.AdViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 网络图片加载组件（带缓存、加载/错误状态）
 */
@Composable
fun NetworkImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    placeholderResId: Int = R.drawable.refresh_24px,
    errorResId: Int = R.drawable.baseline_error_24
) {
    var imageState by remember {
        val cachedBitmap = FilesUtils.getCacheImage(imageUrl)
        mutableStateOf(
            if (cachedBitmap != null) ImageState.Success(cachedBitmap)
            else ImageState.Loading
        )
    }

    if (imageState is ImageState.Loading) {
        LaunchedEffect(imageUrl) {
            withContext(Dispatchers.IO) {
                try {
                    val url = URL(imageUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connect()

                    val bitmap = android.graphics.BitmapFactory.decodeStream(connection.inputStream)
                    FilesUtils.putCacheImage(imageUrl, bitmap)

                    withContext(Dispatchers.Main) {
                        imageState = ImageState.Success(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        imageState = ImageState.Error
                    }
                }
            }
        }
    }

    when (val state = imageState) {
        is ImageState.Loading -> Image(
            painter = painterResource(id = placeholderResId),
            contentDescription = null,
            modifier = modifier
        )
        is ImageState.Success -> Image(
            painter = BitmapPainter(state.bitmap.asImageBitmap()),
            contentDescription = null,
            modifier = modifier
        )
        is ImageState.Error -> Image(
            painter = painterResource(id = errorResId),
            contentDescription = null,
            modifier = modifier
        )
    }
}

/**
 * 封装WebView的Compose组件（优化内存管理）
 */
/**
 * 封装WebView的Compose组件（优化内存管理与渲染性能）
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AdWebView(
    modifier: Modifier = Modifier,
    url: String,
    onCreated: (WebView) -> Unit = {}
) {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // 🌟 WebView 体验与性能核心优化设置
            settings.apply {
                // 1. 脚本与存储支持
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true

                // 2. 缓存策略（兼顾速度与时效）
                cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

                // 3. 页面适应与混合内容处理
                useWideViewPort = true
                loadWithOverviewMode = true
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                // 4. 安全与原生交互感
                allowFileAccess = false
                allowContentAccess = false
                setSupportZoom(false) // 禁用手动缩放，更接近原生 App 体验
            }

            webViewClient = WebViewClient() // 防止跳转到系统浏览器
            webChromeClient = WebChromeClient()
            onCreated(this)
        }
    }

    // 生命周期管理：确保 Compose 组件销毁时释放 WebView 资源
    DisposableEffect(webView) {
        webView.loadUrl(url)
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { webView }
    )
}

/**
 * 广告项组件
 */
@Composable
fun AdItem(ad: com.example.audioandvideoeditor.utils.AdContent, adIndex: Int) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { FilesUtils.openWebLink(context, ad.clickUrl) },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            NetworkImage(
                imageUrl = ad.imageUrl,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ad.title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ad.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 插屏广告弹窗
 */
@Composable
fun InterstitialAdDialog(
    adViewModel: AdViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val isIgnoringBatteryGranted by observeIgnoringBatteryPermissionStatus()

    LaunchedEffect(Unit) {
        webViewInstance = adViewModel.getWebViewInstance()
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(
            text=stringResource(id = R.string.task_tip),
            style = MaterialTheme.typography.headlineMedium
        ) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isIgnoringBatteryGranted) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(stringResource(id = R.string.cancel_power_saving2))
                    Spacer(modifier = Modifier.height(10.dp))
                }

                webViewInstance?.let { webView ->
                    Text(
                        text = stringResource(id = R.string.github_star_dialog_message),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .fillMaxHeight(0.7f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { webView }
                        )
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Transparent)
                                .clickable {
                                    FilesUtils.openWebLink(
                                        context,
                                        context.getString(R.string.link)
                                    )
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {}
    )
}



/**
 * 插屏广告/Star 祈求弹窗组件
 */
@Composable
fun InterstitialAdDialog2(
    type:Int,
    onDismiss: () -> Unit
) {
    //已经展示了，变更标志
    //    AppApplication.INSTANCE.adManager.showADFlag=false
    val context = LocalContext.current
    // 安全获取预加载的 WebView
    val webView = remember {
        AppApplication.INSTANCE.adManager.getWebViewInstance()
    }

    // 🌟 1. 铺满物理屏幕的全局遮罩 (包含状态栏与导航栏区域)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent) // 商业级 透明
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }, // 点击空白处销毁
        contentAlignment = Alignment.Center
    ) {
        // 🌟 2. 对标商业卡片的黄金比例：宽 85%，高 68%
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.68f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* 拦截点击冒泡 */ },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // 彻底无阴影
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header：标题 + 右上角关闭 X
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text =
                            if(type==0){
                                stringResource(id = R.string.task_tip)
                            }
                        else{
                                stringResource(id = R.string.task_completed_message)
                            }
                            ,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // 🌟 2. 引导说明文字（位于 Header 下方、WebView 上方）
                Text(
                    text = stringResource(id = R.string.github_star_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                // Content：WebView 保持内部滚动能力
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (webView != null) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { webView }
                        )
                    } else {
                        // 网络异常或未预加载完成时的提示
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Footer：底部 CTA (Call To Action) 按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            FilesUtils.openWebLink(context, context.getString(R.string.link))
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.visit_github_repo),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}



/**
 * 好物驿站 - 导流插屏弹窗组件
 * 专为商业化/好物专区设计的插屏弹窗，引导用户进入 GoodsExpressActivity 专区
 */
@Composable
fun GoodsExpressAdDialog(
    type: Int,
    onDismiss: () -> Unit,
    onNavigateToGoodsExpress: () -> Unit
) {
    val context = LocalContext.current
    // 安全获取预加载的 WebView
    val webView = remember {
        AppApplication.INSTANCE.adManager.getWebViewInstance()
    }

    // 🌟 1. 铺满物理屏幕的全局遮罩
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // 🌟 2. 商业卡片黄金比例
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.68f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* 拦截点击冒泡 */ },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header：标题 + 右上角关闭按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (type == 0) {
                            stringResource(id = R.string.task_tip)
                        } else {
                            stringResource(id = R.string.task_completed_message)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 🌟 引导说明文字（好物驿站专用文案）
                Text(
                    text = stringResource(id = R.string.goods_express_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

// Content：预加载 WebView 渲染区域（添加阻断覆盖层）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (webView != null) {
                        // 1. 底层：纯展示网页
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { webView }
                        )

                        // 🌟 2. 顶层：透明阻断盖板（拦截所有手势点击与滑动，使用户只能看不能动）
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Transparent)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    // 点击网页区域时，可以选择无响应，或者直接触发跳转专区：
                                     onDismiss()
                                     onNavigateToGoodsExpress()
                                }
                        )
                    } else {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Footer：前往专区 CTA 按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            onDismiss()
                            onNavigateToGoodsExpress()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.enter_goods_express),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}