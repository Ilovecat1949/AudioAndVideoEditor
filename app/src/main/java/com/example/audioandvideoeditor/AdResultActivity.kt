package com.example.audioandvideoeditor

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audioandvideoeditor.components.GoodsExpressAdDialog
import com.example.audioandvideoeditor.components.InterstitialAdDialog
import com.example.audioandvideoeditor.components.InterstitialAdDialog2
import com.example.audioandvideoeditor.ui.theme.AudioAndVideoEditorTheme

// 在 其它位置展示广告弹窗
fun showAdResultActivity(context: Context,type: Int) {
    val intent = Intent(context, AdResultActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("TYPE", type)
    }
    startActivity(context,intent,null)
}
class AdResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 🌟 1. 开启真正的 EdgeToEdge 全屏沉浸
        enableEdgeToEdge()
        // 🌟 2. 确保 Window 铺满整个屏幕物理像素（消灭一切阴影与白边）
        window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        window.setBackgroundDrawableResource(android.R.color.transparent)
        // 1. 获取 Intent 传递的 TYPE 参数（提供默认兜底值）
        val type = intent.getIntExtra("TYPE",0)
        setContent {
            AudioAndVideoEditorTheme {
                // 🌟 直接挂载弹窗组件，不再包裹 Scaffold
                GoodsExpressAdDialog(
                    type = type,
                    onDismiss = { finish() },
                    onNavigateToGoodsExpress = {
                        // 直接拉起独立 Activity
                        val intent = Intent(this, GoodsExpressActivity::class.java)
                        this.startActivity(intent)
                    }
                )
            }
            // 在 AdResultActivity 的 setContent 内部：
            BackHandler {
                // 监听到系统返回键，直接销毁当前透明 Activity
                finish()
            }
        }
    }
}

@Composable
fun Greeting2(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview2() {
    AudioAndVideoEditorTheme {
        Greeting2("Android")
    }
}