package org.dweb_browser.window.render

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dweb_browser.helper.android.AutoResizeTextContainer
import org.dweb_browser.window.core.constant.WindowBottomBarTheme
import org.dweb_browser.window.core.WindowController
import org.dweb_browser.window.core.constant.WindowPropertyKeys
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * 窗口底部栏
 */
@Composable
internal fun WindowBottomBar(
  win: WindowController, modifier: Modifier = Modifier
) {
  val windowEdge = LocalWindowPadding.current
  val contentColor = LocalWindowControllerTheme.current.bottomContentColor
  Box(
    modifier = modifier
      .height(windowEdge.bottom.dp)
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color.Transparent,
            contentColor.copy(alpha = 0.2f),
          )
        )
      )
  ) {
    val maximize by win.watchedIsMaximized()
    if (maximize) {
      WindowBottomMaximizedBar(win) {
        WindowBottomThemeBar(win)
      }
    } else {
      WindowBottomResizeBar(win) {
        WindowBottomThemeBar(win)
      }
    }
  }
}

/**
 * 可以控制窗口大小的底部控制栏
 */
@Composable
private fun WindowBottomResizeBar(
  win: WindowController,
  content: @Composable () -> Unit,
) {
  val contentColor = LocalWindowControllerTheme.current.bottomContentColor
  val windowEdge = LocalWindowPadding.current
  Row(
    modifier = Modifier.fillMaxSize()
  ) {
    /// 左下角 视窗 Resize
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .width(windowEdge.bottom.dp)
        .windowResizeByLeftBottom(win)
    ) {
      Icon(
        Icons.Rounded.ChevronLeft, contentDescription = "Resize by Left Bottom Corner",
        modifier = Modifier
          .rotate(-45f)
          .align(Alignment.Center),
        tint = contentColor,
      )
    }
    /// 下方 视窗 Resize
    Box(
      modifier = Modifier
        .fillMaxSize()
        .weight(1f, true)
    ) {
      content()
    }
    /// 右下角
    /// 视窗 Resize
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .width(windowEdge.bottom.dp)
        .windowResizeByRightBottom(win)
    ) {
      Icon(
        Icons.Rounded.ChevronRight,
        contentDescription = "Resize by Right Bottom Corner",
        modifier = Modifier
          .rotate(45f)
          .align(Alignment.Center),
        tint = contentColor,
      )
    }
  }
}

@Composable
private fun WindowBottomMaximizedBar(
  win: WindowController,
  content: @Composable () -> Unit,
) {
  val windowEdge = LocalWindowPadding.current
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(start = windowEdge.start.dp, end = windowEdge.end.dp),
  ) {
    content()
  }
}

/**
 * 风格化的底部栏
 */
@Composable
private fun WindowBottomThemeBar(
  win: WindowController,
) {
  val bottomBarTheme by win.watchedState(watchKey = WindowPropertyKeys.BottomBarTheme) { bottomBarTheme }
  when (bottomBarTheme) {
    WindowBottomBarTheme.Immersion -> WindowBottomImmersionThemeBar(win)
    WindowBottomBarTheme.Navigation -> WindowBottomNavigationThemeBar(win)
  }
}

/**
 * 沉浸模式
 * 可以看到应用的基本信息和取消最大化
 */
@Composable
private fun WindowBottomImmersionThemeBar(
  win: WindowController,
) {
  val winTheme = LocalWindowControllerTheme.current
  val coroutineScope = rememberCoroutineScope()
  Row(modifier = Modifier
    .fillMaxSize()
    .pointerInput(Unit) {
      detectTapGestures(onDoubleTap = {
        coroutineScope.launch {
          win.unMaximize()
        }
      })
    }) {
    /// 应用图标
    Spacer(
      modifier = Modifier
        .weight(0.618f)
        .fillMaxHeight()
    )
    /// 应用标题
    WindowBottomBarInfoText(
      Modifier.weight(1f), win
    )
  }
}

/**
 * 导航模式
 *
 */
@Composable
private fun WindowBottomNavigationThemeBar(
  win: WindowController,
) {
  val coroutineScope = rememberCoroutineScope()
  val contentColor = LocalWindowControllerTheme.current.bottomContentColor
  val winPadding = LocalWindowPadding.current
  val bottomBarHeight = winPadding.bottom
  val infoHeight = min(bottomBarHeight * 0.2f, LocalWindowLimits.current.bottomBarBaseHeight)
  val isMaximized by win.watchedIsMaximized()
  Row(modifier = Modifier
    .pointerInput(Unit) {
      detectTapGestures(onDoubleTap = {
        coroutineScope.launch {
          win.unMaximize()
        }
      })
    }
    .fillMaxSize()
    //
  ) {
    /// 返回按钮
    BoxWithConstraints(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
    ) {
      val backPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher;
      val bottomRoundedSize = winPadding.contentRounded.bottomStart
      TextButton(
        onClick = {
          backPressedDispatcher?.onBackPressed()
        },
        contentPadding = PaddingValues(0.dp),
        shape = RectangleShape,
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .fillMaxWidth()
          /// 这里强行将按钮太高，目的是为了将按钮的涟漪效果扩散到content下方的圆角部分
          .requiredSize(
            width = maxWidth, height = maxHeight + (bottomRoundedSize).dp
          )
          .absoluteOffset(y = (-bottomRoundedSize / 2).dp),
      ) {
        Icon(
          Icons.Rounded.ArrowBackIosNew,
          contentDescription = "Go Back",
          tint = contentColor,
          modifier = Modifier
            .padding(((bottomBarHeight - infoHeight) * 0.15).dp)
            .align(Alignment.Bottom)
        )
      }
    }

    /// 右侧
    BoxWithConstraints(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
    ) {

      val bottomRoundedSize = winPadding.contentRounded.bottomEnd
      TextButton(
        onClick = {
          coroutineScope.launch { win.unMaximize() }
        },
        enabled = isMaximized,
        contentPadding = PaddingValues(0.dp),
        shape = RectangleShape,
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .fillMaxWidth()
          /// 这里强行将按钮太高，目的是为了将按钮的涟漪效果扩散到content下方的圆角部分
          .requiredSize(
            width = maxWidth, height = maxHeight + (bottomRoundedSize).dp
          )
          .absoluteOffset(y = (-bottomRoundedSize / 2).dp),
      ) {
        Column(
          modifier = Modifier
            .height(bottomBarHeight.dp)
            .align(Alignment.Bottom),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          /// 取消最大化的提示图标
          if (isMaximized) {
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
            ) {
              Icon(
                Icons.Rounded.UnfoldLess,
                contentDescription = "UnMaximizes the window",
                modifier = Modifier
                  .align(Alignment.Center)
                  .rotate(45f),
                tint = contentColor,
              )
            }
          }

          /// 应用标题
          WindowBottomBarInfoText(
            Modifier
              .weight(0.618f)
              .fillMaxWidth(), win
          )
        }
      }
    }
  }

}

/**
 * 应用标题
 * 会根据容器进行自适应排布
 */
@Composable
fun WindowBottomBarInfoText(modifier: Modifier, win: WindowController) {
  val contentColor = LocalWindowControllerTheme.current.bottomContentColor
  AutoResizeTextContainer(modifier.fillMaxHeight()) {
    val footerText by win.watchedState { owner }
    val textStyle = MaterialTheme.typography.labelSmall
    val textFontSize = min(calc(footerText), textStyle.fontSize.value).sp
    Text(
      text = footerText, style = textStyle.copy(
        color = contentColor,
        fontSize = textFontSize,
        fontFamily = FontFamily.Monospace,
        lineHeight = textFontSize * 1.25f
      ), modifier = Modifier.align(Alignment.Center)
    )
  }
}
