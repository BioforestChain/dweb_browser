package info.bagen.libappmgr.ui.splash

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.google.accompanist.pager.*
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import info.bagen.libappmgr.R
import info.bagen.libappmgr.system.media.MediaType
import info.bagen.libappmgr.utils.AppContextUtil
import info.bagen.libappmgr.utils.FilesUtil
import java.io.File

@OptIn(ExperimentalPagerApi::class)
@Composable
fun SplashView(
    paths: ArrayList<String>,
    activeColor: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
    inactiveColor: Color = activeColor.copy(ContentAlpha.disabled),
    indicatorWidth: Dp = 8.dp
) {
    val pagerState = rememberPagerState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            count = paths.size, state = pagerState, modifier = Modifier.fillMaxSize()
        ) { loadPage ->
            val path = paths[loadPage]
            val type = FilesUtil.getFileType(path)
            when (type) {
                MediaType.Video.name -> {
                    SplashVideoView(path = path)
                }
                else -> {
                    AsyncImage(
                        model = path,
                        contentDescription = null,
                        imageLoader = ImageLoader(AppContextUtil.sInstance!!).newBuilder()
                            .components {
                                add(SvgDecoder.Factory())
                            }.build(),
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth(),
                        alignment = Alignment.TopCenter
                    )
                }
            }
        }

        HorizontalPagerIndicator(
            pagerState = pagerState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(0.dp, 0.dp, 0.dp, 50.dp),
            activeColor = activeColor,
            inactiveColor = inactiveColor,
            indicatorWidth = indicatorWidth
        )
    }
}

@Composable
fun SplashVideoView(path: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        SimpleExoPlayer.Builder(context).build().apply {
            playWhenReady = false
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
        }
    }
    DisposableEffect(Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.Center), factory = { context ->
            PlayerView(context).apply {
                useController = false // 是否显示控制视图
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                this.player = exoPlayer
            }
        }, update = { playerView ->
            playerView.player?.apply {
                this.prepare()
                this.play()
            }
        })
    }) {
        onDispose {
            exoPlayer.release()
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SplashPrivacyDialog(
    openHome: () -> Unit, openWebView: (String) -> Unit, closeApp: () -> Unit
) {
    val showPrivacyDeny = remember { mutableStateOf(false) }

    val transition = updateTransition(showPrivacyDeny.value, "")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray.copy(0.6f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            transition.AnimatedVisibility(
                visible = { show -> !show },
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> 2 * fullHeight },
                    animationSpec = tween(1000)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(50)
                )
            ) {
                SplashPrivacyView(openWebView, openHome) { showPrivacyDeny.value = true }
            }

            transition.AnimatedVisibility(
                visible = { show -> show },
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> 2 * fullHeight },
                    animationSpec = tween(1000)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(50)
                )
            ) {
                SplashPrivacyDeny(closeApp) { showPrivacyDeny.value = false }
            }
        }
    }
}

@Composable
private fun SplashPrivacyView(
    openWebView: (String) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(id = R.string.privacy_title),
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        val annotatedString = buildAnnotatedString {
            append(stringResource(id = R.string.privacy_content_1))
            pushStringAnnotation("ysxy", "https://www.bagen.info/privacy_policy.html")
            withStyle(
                SpanStyle(
                    color = Color.Black, fontWeight = FontWeight.Bold
                )
            ) {
                append(stringResource(id = R.string.privacy_policy))
            }
            pop()
            append(stringResource(id = R.string.privacy_content_2))
        }
        ClickableText(text = annotatedString,
            style = TextStyle(fontSize = 16.sp),
            onClick = { position ->
                //通过下面的tag标识找到对应的位置
                annotatedString.getStringAnnotations("ysxy", start = position, end = position)
                    .firstOrNull()
                    ?.let { annotation -> openWebView(annotation.item) }
            })
        Spacer(modifier = Modifier.height(24.dp))

        BottomButton(
            dismissStr = stringResource(id = R.string.button_not_agree),
            confirmStr = stringResource(id = R.string.button_agree_continue),
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
    }
}

@Composable
private fun BottomButton(
    dismissStr: String, confirmStr: String, onDismiss: () -> Unit, onConfirm: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = dismissStr,
            modifier = Modifier
                .weight(1f)
                .background(Color(251, 251, 251), RoundedCornerShape(6.dp))
                .clickable { onDismiss() }
                .padding(top = 12.dp, bottom = 12.dp),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.width(24.dp))
        Text(text = confirmStr,
            modifier = Modifier
                .weight(1f)
                .background(Color(243, 243, 255), RoundedCornerShape(6.dp))
                .clickable { onConfirm() }
                .padding(top = 12.dp, bottom = 12.dp),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color(14, 80, 252))
    }
}

/**
 * 这个是不同意协议后，弹出的再次确认框
 */
@Composable
private fun SplashPrivacyDeny(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(id = R.string.privacy_title),
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(text = stringResource(id = R.string.privacy_content_deny))
        Spacer(modifier = Modifier.height(24.dp))

        BottomButton(
            dismissStr = stringResource(id = R.string.button_exit_app),
            confirmStr = stringResource(id = R.string.button_i_know),
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
    }
}

