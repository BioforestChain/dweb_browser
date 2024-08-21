package org.dweb_browser.sys.window.render

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.platform.LocalDensity
import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.platform.NativeViewController.Companion.nativeViewController
import org.dweb_browser.helper.platform.rememberPureViewBox
import org.dweb_browser.platform.ios.SecureViewController
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowsManager
import org.dweb_browser.sys.window.core.WindowsManagerState.Companion.watchedState
import org.dweb_browser.sys.window.core.constant.debugWindow
import org.dweb_browser.sys.window.helper.LocalWindowsImeVisible
import platform.UIKit.UIViewController

@Composable
private fun <T : WindowController> WindowsManager<T>.EffectKeyboard() {
//  IOS 目前好像不需要对键盘做额外的处理，它能自己很好地全局定位到input的位置
  val imeVisible = LocalWindowsImeVisible.current.value
  val ime = WindowInsets.ime // 直接使用ime，数据不稳定，会变化，改为imeAnimationTarget就是固定值
  val density = LocalDensity.current
  val view = rememberPureViewBox()

  LaunchedEffect(view, imeVisible, ime) { // WindowInsets.ime 对象并不会变化，所以导致这个重组不会重复执行
    state.imeVisible = imeVisible // 由于小米手机的安全键盘存在问题，会出现WindowInsets.isImeVisible不正确的情况
    state.imeBoundingRect = if (imeVisible) {
      val viewSize = view.getViewSize()
      val imeHeightDp = ime.getBottom(density) / density.density
      PureRect(
        x = 0f,
        y = viewSize.height - imeHeightDp,
        height = imeHeightDp,
        width = viewSize.width,
      )
    } else {
      PureRect.Zero
    }

    // 输入法高度即为 heightDiff
    debugWindow(
      "ManagerState/IME", "imeVisible:${state.imeVisible}, imeBounds:${state.imeBoundingRect}"
    )
  }
}

@Composable
private fun <T : WindowController> WindowsManager<T>.EffectNavigationBar() {
  val maximizedWins by maximizedWinsFlow.collectAsState(maximizedWinsFlow.value)
  DisposableEffect(maximizedWins) {
    val noMaximized = maximizedWins.isEmpty();
    /// 如果有窗口处于全屏模式，将操作系统的导航栏标记为隐藏，反之显示
    nativeViewController.navigationBar(noMaximized)
    debugWindow("navigationBar visible", noMaximized)
    onDispose {
      nativeViewController.navigationBar(true)
    }
  }
}


@OptIn(ExperimentalForeignApi::class)
private val secureVcWM = WeakHashMap<UIViewController, SecureViewController>()

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun <T : WindowController> WindowsManager<T>.EffectSafeModel() {
  val safeMode by watchedState { safeMode }
  val vc = LocalUIViewController.current
  val secureViewController = remember(vc) {
    secureVcWM.getOrPut(vc) {
      SecureViewController(vc = vc, onNewView = null)
    }
  }

  if (safeMode) {
    DisposableEffect(secureViewController) {
      secureViewController.setSafeMode(true)
      onDispose {
        secureViewController.setSafeMode(false)
      }
    }
  }
}

@Composable
actual fun <T : WindowController> WindowsManager<T>.WindowsManagerEffect() {

  /// 键盘的互操作性
  EffectKeyboard()
  /// 底部导航栏的互操作
  EffectNavigationBar()
  /// 窗口截屏安全限制
  EffectSafeModel()
}