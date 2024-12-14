package com.example.audioandvideoeditor.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner

@Composable
fun rememberLifecycle(): ComposeLifecycleObserver {
    val observer= ComposeLifecycleObserver()
    val owner= LocalLifecycleOwner.current
    DisposableEffect(key1 = "lifecycle", effect = {
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
        }
    })
    val ctx= LocalContext.current
    return remember(ctx) {
        observer
    }
}