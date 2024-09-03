package org.dweb_browser.sys.window.render

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.compose.CompositionChain
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.asDesktop
import org.dweb_browser.helper.platform.rememberPureViewBox
import org.dweb_browser.sys.window.core.LocalWindowsManager
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowsManager
import org.dweb_browser.sys.window.core.constant.debugWindow
import org.dweb_browser.sys.window.helper.watchedIsFullscreen

@Composable
actual fun <T : WindowController> WindowsManager<T>.SceneRender(modifier: Modifier) {
  val windowsManager = this
  BoxWithConstraints(modifier) {
    WindowsManagerEffect()
    /// 普通层级的窗口
    val winList by winListFlow.collectAsState()
    debugWindow("WindowsManager.Render", "winList: ${winList.size}")
    for (win in winList) {
      key(win.id) {
        RenderWindowInNative(windowsManager, win)
      }
    }
    /// 置顶层级的窗口
    val topWinList by topWinListFlow.collectAsState()
    debugWindow("WindowsManager.Render", "winListTop: ${topWinList.size}")
    for (win in topWinList) {
      key(win.id) {
        RenderWindowInNative(windowsManager, win)
      }
    }
  }
}

@Composable
fun RenderWindowInNative(
  windowsManager: WindowsManager<*>,
  win: WindowController,
) {
  val maxBounds = rememberPureViewBox().asDesktop()
    .currentViewControllerMaxBounds(withSafeArea = !win.watchedIsFullscreen().value)
  win.Prepare(
    winMaxWidth = maxBounds.width, winMaxHeight = maxBounds.height, minScale = 1.0
  ) {
    val compositionChain = rememberUpdatedState(LocalCompositionChain.current)
    val pvc = win.getDesktopWindowNativeView(windowsManager, compositionChain).pvc

    /// 启动
    LaunchedEffect(pvc) {
      if (!pvc.composeWindowParams.isOpened) {
        // 使用系统原生的窗口进行渲染
        win.state.renderConfig.isSystemWindow = true
        win.state.renderConfig.isWindowUseComposeFrame = false
        pvc.composeWindowParams.apply {
          // 打开窗口
          openWindow()
        }
      }
    }
    /// 对屏幕键盘的适配
    /// TODO 需要设备开启虚拟键盘后进行测试完善该功能
    /// 需要确保上游计算厂商提供了屏幕键盘到compose的适配工作
    //    val windowImeOutsetBounds = calcWindowImeOutsetBounds(windowsManager, win)
    //    win.state.keyboardInsetBottom = windowImeOutsetBounds.keyboardInsetBottom
    //    var winOffsetByIme by remember { mutableStateOf(0) }
    //    val composeWindow by pvc.composeWindowAsState()
    //    LaunchedEffect(windowImeOutsetBounds.modifierOffsetY) {
    //      when(val locationOffsetY = windowImeOutsetBounds.modifierOffsetY.toInt()){
    //        0f->
    //      }
    //      composeWindow.setLocation(
    //        composeWindow.x,
    //        composeWindow.y + winOffsetByIme + windowImeOutsetBounds.modifierOffsetY.toInt()
    //      )
    //      winOffsetByIme += windowImeOutsetBounds.modifierOffsetY.toInt()
    //    }
  }
}

private fun WindowController.getDesktopWindowNativeView(
  windowsManager: WindowsManager<*>, compositionChain: State<CompositionChain>,
) = DesktopWindowNativeView.INSTANCES.getOrPut(this) {
  DesktopWindowNativeView(
    mutableMapOf(
      "compositionChain" to compositionChain,
    ), this, windowsManager
  )
}

private class DesktopWindowNativeView(
  params: Map<String, Any?>,
  private val win: WindowController,
  private val windowsManager: WindowsManager<*>,
) {
  val pvc = PureViewController(params).also { pvc ->
    pvc.onCreate { params ->
      // TODO 将Effect中的onChange绑定在这里做初始化
      @Suppress("UNCHECKED_CAST") pvc.addContent {
        val compositionChain by params["compositionChain"] as State<CompositionChain>

        (compositionChain + LocalCompositionChain.current).Provider(LocalWindowsManager provides windowsManager) {
          /// 注册副作用
          win.WindowControllerEffect()
          /// 渲染窗口
          win.WithMaterialTheme {
            win.WindowRender(Modifier)
          }
        }
      }
    }
  }

  companion object {
    val INSTANCES = WeakHashMap<WindowController, DesktopWindowNativeView>()
  }
}