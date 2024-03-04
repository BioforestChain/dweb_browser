package org.dweb_browser.sys.window.render

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.compose.CompositionChain
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.platform.desktop.window.EmptyWindowListener
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowsManager
import org.dweb_browser.sys.window.core.WindowsManagerState.Companion.windowImeOutsetBounds
import org.dweb_browser.sys.window.core.constant.debugWindow
import javax.swing.JFrame

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
        RenderWindowInNative(windowsManager, win, maxWidth.value, maxHeight.value)
      }
    }
    /// 置顶层级的窗口
    debugWindow("WindowsManager.Render", "winListTop: ${winListTop.size}")
    for (win in winListTop) {
      key(win.id) {
        RenderWindowInNative(windowsManager, win, maxWidth.value, maxHeight.value)
      }
    }
  }
}

fun WindowController.openPvcInNativeWindow(pvc: PureViewController) =
  JFrame(this.state.title).also { frame ->
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE; // 设置关闭按钮的动作
    frame.isVisible = true; // 设置窗口可见

    frame.setLocation(state.bounds.x.toInt(), state.bounds.y.toInt()); // 设置窗口位置
    frame.setSize(state.bounds.width.toInt(), state.bounds.height.toInt()); // 设置窗口大小
    frame.isAlwaysOnTop = state.alwaysOnTop
    frame.add(pvc.getJPanel())
  }

@Composable
fun RenderWindowInNative(
  windowsManager: WindowsManager<*>,
  win: WindowController,
  currentMaxWidth: Float,
  currentMaxHeight: Float,
) {

  val maxWidth = rememberUpdatedState(currentMaxWidth)
  val maxHeight = rememberUpdatedState(currentMaxHeight)
  val compositionChain = rememberUpdatedState(LocalCompositionChain.current)
  val pvc =
    win.getDesktopWindowNativeView(windowsManager, maxWidth, maxHeight, compositionChain).pvc

  /// 启动
  DisposableEffect(pvc) {
    val panel = win.openPvcInNativeWindow(pvc)
    panel.addWindowListener(object : EmptyWindowListener() {

    })
    val off = win.onClose {
      panel.dispose()
    }
    onDispose {
      off()
    }
  }
}

private fun WindowController.getDesktopWindowNativeView(
  windowsManager: WindowsManager<*>,
  maxWidth: State<Float>,
  maxHeight: State<Float>,
  compositionChain: State<CompositionChain>
) = DesktopWindowNativeView.INSTANCES.getOrPut(this) {
  DesktopWindowNativeView(
    mutableMapOf(
      "maxWidth" to maxWidth,
      "maxHeight" to maxHeight,
      "compositionChain" to compositionChain,
    ), this, windowsManager
  )
}

private class DesktopWindowNativeView(
  params: Map<String, Any?>,
  private val win: WindowController,
  private val windowsManager: WindowsManager<*>
) {
  val pvc = PureViewController(params).also { pvc ->
    pvc.onCreate { params ->
      @Suppress("UNCHECKED_CAST") pvc.addContent {
        val maxWidth by params["maxWidth"] as State<Float>
        val maxHeight by params["maxHeight"] as State<Float>
        val compositionChain by params["compositionChain"] as State<CompositionChain>
        compositionChain.Provider(LocalCompositionChain.current)
          .Provider(LocalWindowsManager provides windowsManager) {
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

  companion object {
    val INSTANCES = WeakHashMap<WindowController, DesktopWindowNativeView>()
  }
}