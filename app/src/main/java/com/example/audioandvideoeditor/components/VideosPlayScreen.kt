package com.example.audioandvideoeditor.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.viewmodel.VideosPlayViewModel

@Composable
fun VideosPlayScreen(
    modifier: Modifier,
    path_or_uri:String,
    videosPlayViewModel: VideosPlayViewModel= viewModel()
){
val life= rememberLifecycle()
videosPlayViewModel.initExoPlayer(LocalContext.current)
life.onLifeCreate {
    val mediaItem = MediaItem.fromUri(Uri.parse(path_or_uri))
    videosPlayViewModel.exoplayer.setMediaItem(mediaItem)
    videosPlayViewModel.exoplayer.prepare()
}
life.onLifeStop {
    videosPlayViewModel.exoplayer.pause()
    videosPlayViewModel.exoplayer.stop()
    videosPlayViewModel.exoplayer.release()
}
life.onLifeDestroy {
    videosPlayViewModel.exoplayer.pause()
    videosPlayViewModel.exoplayer.stop()
    videosPlayViewModel.exoplayer.release()
}
    PlayerSurface(modifier){
        it.player=videosPlayViewModel.exoplayer
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