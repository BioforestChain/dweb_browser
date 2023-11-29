package org.dweb_browser.sys.window.render

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
  val maxWidth = rememberUpdatedState(currentMaxWidth)
  val maxHeight = rememberUpdatedState(currentMaxHeight)
  val params = remember {
    mutableMapOf(
      "maxWidth" to maxWidth,
      "maxHeight" to maxHeight,
    )
  }
  val microModule = LocalWindowMM.current
  val pvc = remember {
    PureViewController(params).also { pvc ->
      pvc.onCreate { params ->
        @Suppress("UNCHECKED_CAST") pvc.addContent {
          val maxWidth by params["maxWidth"] as State<Float>
          val maxHeight by params["maxHeight"] as State<Float>
          // TODO: 等待 currentCompositionLocalContext 可用
          // https://youtrack.jetbrains.com/issue/KT-63869/androidx.compose.runtime.ComposeRuntimeError-Compose-Runtime-internal-error.-Unexpected-or-incorrect-use-of-the-Compose-internal
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
    /// 窗口截屏安全限制
    EffectSafeModel()
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