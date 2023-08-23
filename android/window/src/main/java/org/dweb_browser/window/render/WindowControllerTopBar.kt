package org.dweb_browser.window.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Minimize
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dweb_browser.helper.android.AutoResizeTextContainer
import org.dweb_browser.helper.android.AutoSizeText
import org.dweb_browser.window.core.WindowController
import kotlinx.coroutines.launch


@Composable
internal fun WindowTopBar(
  win: WindowController,
) {
  val winPadding = LocalWindowPadding.current;
  val contentColor = LocalWindowControllerTheme.current.topContentColor
  Box(
    modifier = Modifier
      .windowMoveAble(win)
      .fillMaxWidth()
      .height(winPadding.top.dp)
      .background(
        Brush.verticalGradient(
          colors = listOf(
            contentColor.copy(alpha = 0.2f),
            Color.Transparent,
          )
        )
      )
  ) {
    val maximize by win.watchedIsMaximized()
    if (maximize) {
      WindowTopMaximizedBar(win)
    } else {
      WindowTopControllerBar(win)
    }
  }
}

@Composable
private fun WindowTopControllerBar(
  win: WindowController,
) {
  val coroutineScope = rememberCoroutineScope()
  val contentColor = LocalWindowControllerTheme.current.topContentColor
  val topBarHeight = LocalWindowPadding.current.top
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    /// 关闭按钮
    Box(
      modifier = Modifier
        .width(topBarHeight.dp)
        .fillMaxHeight(),
    ) {
      IconButton(modifier = Modifier.align(Alignment.Center),
        onClick = { coroutineScope.launch { win.close() } }) {
        Icon(Icons.Rounded.Close, contentDescription = "Close the Window", tint = contentColor)
      }
    }
    /// 应用图标
    val iconSize = topBarHeight * 0.8f;
    Box(
      modifier = Modifier
        .size(iconSize.dp),
    ) {
      win.IconRender(
        modifier = Modifier
          .align(Alignment.CenterStart)
          .fillMaxSize(), primaryColor = contentColor
      )
    }
    /// 标题信息
    AutoResizeTextContainer(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight(),
    ) {
      val inResize by win.inResize
      val titleText by win.watchedState { title ?: owner }

      val baseFontStyle = MaterialTheme.typography.titleSmall
      val fontStyle = remember(contentColor) {
        baseFontStyle.copy(color = contentColor)
      }

      AutoSizeText(
        modifier = Modifier
          .align(Alignment.Center)
          .padding(2.dp),
        text = titleText,
        textAlign = TextAlign.Center,
        style = fontStyle,
        autoResizeEnabled = !inResize,
        autoLineHeight = { (1.5f * it.value).sp }
      )
    }
    /// 右侧的控制按钮

    /// 最小化
    Box(
      modifier = Modifier
        .width(topBarHeight.dp)
        .fillMaxHeight(),
    ) {
      IconButton(modifier = Modifier.align(Alignment.CenterEnd),
        onClick = { coroutineScope.launch { win.minimize() } }) {
        Icon(
          Icons.Rounded.Minimize,
          contentDescription = "Minimizes the window",
          tint = contentColor
        )
      }
    }
    /// 最大化
    Box(
      modifier = Modifier
        .width(topBarHeight.dp)
        .fillMaxHeight(),
    ) {
      IconButton(modifier = Modifier.align(Alignment.CenterEnd),
        onClick = { coroutineScope.launch { win.maximize() } }) {
        Icon(
          Icons.Rounded.UnfoldMore,
          contentDescription = "Maximizes the window",
          modifier = Modifier.rotate(45f),
          tint = contentColor
        )
      }
    }
  }
}


/**
 * 最大化模式下，顶部默认不会有东西，因为这里针对移动设备进行设计
 * 这时候的顶部与 save-area-top 一致
 */
@Composable
private fun WindowTopMaximizedBar(
  win: WindowController,
) {
  /// 这里可以渲染一些特殊的信息，比如将应用图标渲染到状态栏中
  //  val context = LocalContext.current
  //  SideEffect {
  //    if (ActivityCompat.checkSelfPermission(
  //        context,
  //        Manifest.permission.POST_NOTIFICATIONS
  //      ) != PackageManager.PERMISSION_GRANTED
  //    ) {
  //      when (val activity = win.context) {
  //        is BaseActivity -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
  //          activity.lifecycleScope.launch {
  //            activity.requestSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
  //          }
  //        }
  //
  //        else -> {}
  //      }
  //    }
  //  }
  //  val easyBitmap by remember {
  //    val easyBitmapState = mutableStateOf<Bitmap?>(null)
  //
  //    ComposeView(context).also { composeView ->
  //      composeView.setContent {
  //        win.IconRender(modifier = Modifier.size(64.dp, 64.dp))
  //      }
  //      val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
  //
  //      bitmap.applyCanvas {
  //        composeView.draw(this)
  //
  //        easyBitmapState.value = bitmap
  //
  //        /// 推送通知
  //        val builder =
  //          NotificationCompat.Builder(composeView.context, NotificationChannel.DEFAULT_CHANNEL_ID)
  //            .setSmallIcon(IconCompat.createWithBitmap(bitmap))
  //            .setContentTitle("Notification Title")
  //            .setContentText("Notification Content")
  //            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
  //
  //        val notification = builder.build()
  //
  //        NotificationManagerCompat.from(composeView.context).notify(1, notification)
  //
  //      }
  //
  //    }
  //    easyBitmapState
  //  }
  //  if (easyBitmap != null) {
  //    Image(easyBitmap!!.asImageBitmap(), contentDescription = "??")
  //  }
}

