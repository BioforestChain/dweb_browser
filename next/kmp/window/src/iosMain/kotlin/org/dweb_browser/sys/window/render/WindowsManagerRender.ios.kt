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
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.nativeRootUIViewController_addOrUpdate
import org.dweb_browser.helper.platform.nativeRootUIViewController_remove
import org.dweb_browser.helper.platform.nativeRootUIViewController_scope
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowsManager
import org.dweb_browser.sys.window.core.WindowsManagerState.Companion.windowImeOutsetBounds
import org.dweb_browser.sys.window.core.constant.debugWindow


@Composable
fun RenderWindowInNewLayer(
  win: WindowController,
  maxWidth: Float,
  maxHeight: Float,
  zIndexBase: Int
) {
  val nativeScope = nativeRootUIViewController_scope
  val params = remember {
    mutableMapOf(
      "maxWidth" to mutableStateOf(maxWidth),
      "maxHeight" to mutableStateOf(maxHeight),
    )
  }
  LaunchedEffect(maxWidth, maxHeight) {
    params["maxWidth"]!!.value = maxWidth
    params["maxHeight"]!!.value = maxHeight
  }
  val pvc = remember {
    PureViewController(params).also { pvc ->
      pvc.onCreate { params ->
        val maxWidth by params["maxWidth"] as State<Float>
        val maxHeight by params["maxHeight"] as State<Float>
        pvc.addContent {
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
  val zIndex by win.watchedState(zIndexBase) { zIndex + zIndexBase }
  /// 启动
  DisposableEffect(pvc) {
    nativeScope.launch {
      nativeRootUIViewController_addOrUpdate(pvc, zIndex)
    }
    onDispose {
      nativeScope.launch { // win.onClose
        nativeRootUIViewController_remove(pvc)
      }
    }
  }
  /// 切换zIndex
  LaunchedEffect(zIndex) {
    nativeRootUIViewController_addOrUpdate(pvc, zIndex)
  }
}

@Composable
actual fun <T : WindowController> WindowsManager<T>.Render() {
  CompositionLocalProvider(
    LocalWindowsManager provides this,
  ) {
    BoxWithConstraints {
      /// 键盘的互操作性
      EffectKeyboard()
      /// 底部导航栏的互操作
      EffectNavigationBar()
      /// 普通层级的窗口
      debugWindow("WindowsManager.Render", "winList: ${winList.size}")
      for (win in winList) {
        key(win.id) {
          RenderWindowInNewLayer(win, maxWidth.value, maxHeight.value, 0)
        }
      }
      /// 置顶层级的窗口
      debugWindow("WindowsManager.Render", "winListTop: ${winListTop.size}")
      for (win in winListTop) {
        key(win.id) {
          RenderWindowInNewLayer(win, maxWidth.value, maxHeight.value, 10000)
        }
      }
    }
  }
}