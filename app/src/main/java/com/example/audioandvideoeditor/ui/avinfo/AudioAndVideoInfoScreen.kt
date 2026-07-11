package com.example.audioandvideoeditor.ui.avinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.utils.TextsUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AVInfoScreen(
    path:String,
    avInfoViewModel: AudioAndVideoInfoViewModel= viewModel()
){
    val life= rememberLifecycle()
    life.onLifeCreate {
        avInfoViewModel.getInfo(path)
    }
        val file= File(path)
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
        ){
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ){
                    showFileAttributeItem(stringResource(id = R.string.file_name),file.name)

                    showFileAttributeItem(stringResource(id = R.string.path),file.path)

                    showFileAttributeItem(stringResource(id = R.string.size), TextsUtils.getSizeText(file.length()))
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                    showFileAttributeItem(stringResource(id = R.string.date),sdf.format(file.lastModified()))

                    showFileAttributeItem(stringResource(id = R.string.extension),file.extension)

                }
            }
            if(avInfoViewModel.info.video_bit_rate>0L) {
                item {
                    showFileAttributeItem(stringResource(id = R.string.video_duration), TextsUtils.millisecondsToString(avInfoViewModel.info.video_duration*1000))
                    showFileAttributeItem(
                        stringResource(id = R.string.video_resolution),
                        "${avInfoViewModel.info.width}x${avInfoViewModel.info.height}"
                    )

                    showFileAttributeItem(
                        "${stringResource(id = R.string.video_bit_rate)}(kb/s)",
                        "${avInfoViewModel.info.video_bit_rate / 1000}"
                    )

                    showFileAttributeItem("${stringResource(id = R.string.frame_rate)}(fps)", "${avInfoViewModel.info.frame_rate}")

                    showFileAttributeItem(stringResource(id = R.string.video_encoding_format), avInfoViewModel.info.video_codec_type)

                }
            }
            if(avInfoViewModel.info.audio_bit_rate>0L) {
                item {
                    showFileAttributeItem(stringResource(id = R.string.audio_duration), TextsUtils.millisecondsToString(avInfoViewModel.info.audio_duration*1000))
                    showFileAttributeItem(stringResource(id = R.string.audio_format), avInfoViewModel.info.audio_codec_type)
                    showFileAttributeItem(
                        "${stringResource(id = R.string.audio_bit_rate)}(kb/s)",
                        "${avInfoViewModel.info.audio_bit_rate / 1000}"
                    )

                    showFileAttributeItem("${stringResource(id = R.string.sample_rate)}(HZ)", "${avInfoViewModel.info.sample_rate}")

                }
            }
        }
}

@Composable
private fun showFileAttributeItem(
    attribute:String,
    value:String
){
    Column (
        modifier = Modifier
            .fillMaxWidth()
    ){
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(attribute  ,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
            )
            {
                Text(value)
                Spacer(modifier = Modifier.width(10.dp))
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Divider(color = Color.LightGray, thickness = 1.dp)
    }

}
