package org.dweb_browser.sys.window.render

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.helper.compose.AutoResizeTextContainer
import org.dweb_browser.helper.compose.AutoSizeText
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.helper.LocalWindowControllerTheme
import org.dweb_browser.sys.window.helper.LocalWindowFrameStyle
import org.dweb_browser.sys.window.helper.watchedState

// 这里是窗口顶部，分成移动端和桌面端实现，桌面端将使用原生的窗口并且绑定事件
@Composable
expect fun WindowTopBar(win: WindowController, modifier: Modifier)

// 平常情况下的顶部窗口
@Composable
fun WindowTopControllerBar(
  win: WindowController,
) {
  val scope = rememberCoroutineScope()
  val contentColor = LocalWindowControllerTheme.current.topContentColor
  val topBarHeight = LocalWindowFrameStyle.current.frameSize.top
  Box {
    /// 菜单面板居中定位，主要为了适配锚点，目前范围是窗口的正行
    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
      WindowMenuPanel(win)
    }
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
          onClick = {
            scope.launch {
              win.tryCloseOrHide()
            }
          }) {
          Icon(Icons.Rounded.Close, contentDescription = "Close the Window", tint = contentColor)
        }
      }
      /// 应用图标
      val iconSize = topBarHeight * 0.8f;
      Box(
        modifier = Modifier
          .size(iconSize.dp)
          .clickable { scope.launch { win.showMenuPanel() } },
      ) {
        win.IconRender(
          modifier = Modifier
            .align(Alignment.CenterStart)
            .fillMaxSize(),
          primaryColor = contentColor
        )
      }
      /// 标题信息
      AutoResizeTextContainer(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .clickable { scope.launch { win.showMenuPanel() } },
      ) {
        val inResize by win.inResize
        val titleText by win.watchedState { title ?: constants.owner }

        val baseFontStyle = MaterialTheme.typography.titleSmall
        val fontStyle = remember(baseFontStyle, contentColor) {
          baseFontStyle.copy(color = contentColor)
        }

        val maxFontSize = baseFontStyle.fontSize
        AutoSizeText(modifier = Modifier
          .align(Alignment.Center)
          .padding(2.dp),
          text = titleText,
          textAlign = TextAlign.Center,
          style = fontStyle,
          autoResizeEnabled = !inResize,
          onResize = {
            if (fontSize > maxFontSize) {
              fontSize = maxFontSize
            }
            lightHeight = fontSize * 1.5f
          })
      }

      /// 右侧的控制按钮
      /// 最小化
      Box(
        modifier = Modifier
          .width(topBarHeight.dp)
          .fillMaxHeight(),
      ) {
        IconButton(modifier = Modifier.align(Alignment.CenterEnd),
          onClick = { scope.launch { win.toggleVisible() } }) {
          Icon(
            Icons.Rounded.Minimize, contentDescription = "Minimizes the window", tint = contentColor
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
          onClick = { scope.launch { win.maximize() } }) {
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
}


/**
 * 最大化模式下，顶部默认不会有东西，因为这里针对移动设备进行设计
 * 这时候的顶部与 save-area-top 一致
 */
@Composable
fun WindowTopMaximizedBar(
  @Suppress("UNUSED_PARAMETER") win: WindowController,
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

