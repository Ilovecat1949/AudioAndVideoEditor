package com.example.audioandvideoeditor.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.startActivity
import com.example.audioandvideoeditor.R

import com.example.audioandvideoeditor.utils.FilesUtils
import com.example.audioandvideoeditor.utils.ImageState
import com.example.audioandvideoeditor.utils.observeIgnoringBatteryPermissionStatus
import com.example.audioandvideoeditor.viewmodel.AdViewModel
import com.example.audioandvideoeditor.viewmodel.HomeViewModel
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
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient() // 防止跳转到系统浏览器
            webChromeClient = WebChromeClient()
            onCreated(this)
        }
    }

    // 生命周期管理：确保Compose组件销毁时释放WebView资源
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
    viewModel: HomeViewModel,
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
        title = { Text(stringResource(id = R.string.task_tip)) },
        text = {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isIgnoringBatteryGranted) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(stringResource(id = R.string.cancel_power_saving2))
                    Spacer(modifier = Modifier.height(10.dp))
                }

                webViewInstance?.let { webView ->
                    Text(
                        text = stringResource(id = R.string.ad),
                        style = MaterialTheme.typography.headlineMedium,
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