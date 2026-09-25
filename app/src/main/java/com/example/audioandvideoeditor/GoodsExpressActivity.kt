package com.example.audioandvideoeditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.audioandvideoeditor.components.GoodsExpressScreen
import com.example.audioandvideoeditor.ui.theme.AudioAndVideoEditorTheme

class GoodsExpressActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudioAndVideoEditorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GoodsExpressScreen(
                        mainUrl = stringResource(R.string.link),
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting3(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview3() {
    AudioAndVideoEditorTheme {
        Greeting3("Android")
    }
}