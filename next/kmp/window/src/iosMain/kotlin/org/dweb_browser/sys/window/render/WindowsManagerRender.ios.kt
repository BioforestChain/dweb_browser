package org.dweb_browser.sys.window.render

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.dweb_browser.helper.platform.NativeViewController.Companion.nativeViewController
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowsManager
import org.dweb_browser.sys.window.core.WindowsManagerState.Companion.windowImeOutsetBounds
import org.dweb_browser.sys.window.core.constant.LocalWindowMM
import org.dweb_browser.sys.window.core.constant.debugWindow


@Composable
fun RenderWindowInNewLayer(
  windowsManager: WindowsManager<*>,
  win: WindowController,
  currentMaxWidth: Float,
  currentMaxHeight: Float,
  zIndexBase: Int
) {
  val nativeScope = nativeViewController.scope
  val params = remember {
    mutableMapOf(
      "maxWidth" to mutableStateOf(currentMaxWidth),
      "maxHeight" to mutableStateOf(currentMaxHeight),
    )
  }
  LaunchedEffect(currentMaxWidth, currentMaxHeight) {
    params["maxWidth"]!!.value = currentMaxWidth
    params["maxHeight"]!!.value = currentMaxHeight
  }
  val microModule = LocalWindowMM.current
  val pvc = remember {
    PureViewController(params).also { pvc ->
      pvc.onCreate { params ->
        pvc.addContent {
          @Suppress("UNCHECKED_CAST")
          val maxWidth by params["maxWidth"] as State<Float>

          @Suppress("UNCHECKED_CAST")
          val maxHeight by params["maxHeight"] as State<Float>
          CompositionLocalProvider(
            LocalWindowsManager provides windowsManager,
            LocalWindowMM provides microModule,
          ) {
            /// 渲染窗口
            win.Render(
              modifier = Modifier.windowImeOutsetBounds(),
              maxWinWidth = maxWidth,
              maxWinHeight = maxHeight
            )
          }
        }
      }
    }
  }
  val zIndex by win.watchedState(zIndexBase) { zIndex + zIndexBase }
  /// 启动
  DisposableEffect(pvc) {
    nativeScope.launch {
      nativeViewController.addOrUpdate(pvc, zIndex)
    }
    onDispose {
      nativeScope.launch { // win.onClose
        nativeViewController.remove(pvc)
      }
    }
  }
  /// 切换zIndex
  LaunchedEffect(zIndex) {
    nativeViewController.addOrUpdate(pvc, zIndex)
  }
}

@Composable
actual fun <T : WindowController> WindowsManager<T>.Render() {
  val windowsManager = this
  BoxWithConstraints {
    /// 键盘的互操作性
    EffectKeyboard()
    /// 底部导航栏的互操作
    EffectNavigationBar()
    /// 普通层级的窗口
    debugWindow("WindowsManager.Render", "winList: ${winList.size}")
    for (win in winList) {
      key(win.id) {
        RenderWindowInNewLayer(windowsManager, win, maxWidth.value, maxHeight.value, 10)
      }
    }
    /// 置顶层级的窗口
    debugWindow("WindowsManager.Render", "winListTop: ${winListTop.size}")
    for (win in winListTop) {
      key(win.id) {
        RenderWindowInNewLayer(windowsManager, win, maxWidth.value, maxHeight.value, 10000)
      }
    }
  }
}