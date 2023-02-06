package info.bagen.libappmgr.ui.dcim

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import info.bagen.libappmgr.R
import info.bagen.libappmgr.entity.ExoPlayerData
import info.bagen.libappmgr.entity.PlayerState
import java.io.File

@SuppressLint("UnrememberedMutableState")
@Composable
fun VideoScreen(dcimVM: DCIMViewModel, path: String) {
    val context = LocalContext.current
    val exoPlayerData = remember {
        ExoPlayerData(
            exoPlayer = SimpleExoPlayer.Builder(context).build().apply {
                playWhenReady = false
                setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
            },
            playerState = mutableStateOf(PlayerState.Play)
        )
    }

    exoPlayerData.exoPlayer.addAnalyticsListener(object : AnalyticsListener {
        override fun onPlayWhenReadyChanged(
            eventTime: AnalyticsListener.EventTime,
            playWhenReady: Boolean,
            reason: Int
        ) {
            when (playWhenReady) {
                true -> exoPlayerData.playerState.value = PlayerState.Playing
                false -> exoPlayerData.playerState.value = PlayerState.Pause
            }
        }

        override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
            when (state) {
                Player.STATE_ENDED -> exoPlayerData.playerState.value = PlayerState.END
            }
        }
    })

    DisposableEffect(
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                factory = { context ->
                    dcimVM.handlerIntent(DCIMIntent.AddExoPlayer(exoPlayerData))
                    PlayerView(context).apply {
                        useController = false // 是否显示控制视图
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        this.player = exoPlayerData.exoPlayer
                    }
                },
                update = { playerView ->
                    playerView.player?.apply {
                        this.prepare()
                        this.pause()  // 为了保证每次点击视频后不主动播放
                        //dcimVM.exoPlayerList.add(this as SimpleExoPlayer)
                    }
                }
            )
            PlayerControlView(exoPlayerData)
        }
    ) {
        onDispose {
            dcimVM.handlerIntent(DCIMIntent.RemoveExoPlayer(exoPlayerData))
        }
    }
}


@Composable
fun BoxScope.PlayerControlView(epd: ExoPlayerData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .align(Alignment.Center)
            .clickable(
                onClick = {
                    when (epd.playerState.value) {
                        PlayerState.Play -> epd.exoPlayer.play()
                        PlayerState.Playing -> epd.exoPlayer.pause()
                        PlayerState.Pause -> epd.exoPlayer.play()
                        PlayerState.END -> {
                            epd.exoPlayer.seekTo(0)
                            epd.exoPlayer.play()
                            epd.playerState.value = PlayerState.Playing
                        }
                    }
                },
                // 去除水波纹效果
                indication = null,
                interactionSource = remember { MutableInteractionSource() })
    ) {
        AsyncImage(
            model = when (epd.playerState.value) {
                PlayerState.Play -> R.drawable.ic_player_play
                PlayerState.Pause -> R.drawable.ic_player_pause
                PlayerState.END -> R.drawable.ic_player_replay
                PlayerState.Playing -> null
            },
            contentDescription = null,
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.Center)
        )
    }
}
