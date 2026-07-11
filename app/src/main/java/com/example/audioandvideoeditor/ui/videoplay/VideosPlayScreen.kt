package com.example.audioandvideoeditor.ui.videoplay

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle

@Composable
fun VideoPlayScreen(
    modifier: Modifier,
    path_or_uri:String,
    videoPlayViewModel: VideoPlayViewModel= viewModel()
){
val life= rememberLifecycle()
videoPlayViewModel.initExoPlayer(LocalContext.current)
life.onLifeCreate {
    val mediaItem = MediaItem.fromUri(Uri.parse(path_or_uri))
    videoPlayViewModel.exoplayer.setMediaItem(mediaItem)
    videoPlayViewModel.exoplayer.prepare()
}
life.onLifeStop {
    videoPlayViewModel.exoplayer.pause()
    videoPlayViewModel.exoplayer.stop()
    videoPlayViewModel.exoplayer.release()
}
life.onLifeDestroy {
    videoPlayViewModel.exoplayer.pause()
    videoPlayViewModel.exoplayer.stop()
    videoPlayViewModel.exoplayer.release()
}
    PlayerSurface(modifier){
        it.player=videoPlayViewModel.exoplayer
    }
}




@Composable
private fun PlayerSurface(
    modifier: Modifier,
    onPlayerViewAvailable: (PlayerView) -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                useController =true
                onPlayerViewAvailable(this)
            }
        },
        modifier = modifier
    )
}