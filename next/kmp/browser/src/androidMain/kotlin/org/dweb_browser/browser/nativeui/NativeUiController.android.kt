package org.dweb_browser.browser.nativeui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import org.dweb_browser.browser.nativeui.helper.QueryHelper
import org.dweb_browser.browser.nativeui.navigationBar.NavigationBarController
import org.dweb_browser.browser.nativeui.safeArea.SafeAreaController
import org.dweb_browser.browser.nativeui.statusBar.StatusBarController
import org.dweb_browser.browser.nativeui.virtualKeyboard.VirtualKeyboardController
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.PureViewController

actual class NativeUiController actual constructor(pureViewController: IPureViewController) {
  val activity = (pureViewController as PureViewController)

  val windowInsetsController by lazy {
    WindowCompat.getInsetsController(
      activity.window, activity.window.decorView
    )
  }
  val currentInsets =
    mutableStateOf(WindowInsetsCompat.toWindowInsetsCompat(activity.window.decorView.rootWindowInsets))

  fun getCurrentInsets(typeMask: Int) = currentInsets.value.getInsets(typeMask)

  val statusBar = StatusBarController(activity, this)
  val navigationBar = NavigationBarController(activity, this)
  val virtualKeyboard = VirtualKeyboardController(activity, this)

  val safeArea = SafeAreaController(activity, this)


  @Composable
  actual fun effect(): NativeUiController {
    /**
     * 这个 NativeUI 的逻辑是工作在全屏幕下，所以会使得默认覆盖 系统 UI
     */
    SideEffect {
      WindowCompat.setDecorFitsSystemWindows(activity.window, false)
      /// system-bar 一旦隐藏（visible = false），那么被手势划出来后，过一会儿自动回去
      windowInsetsController.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

      ViewCompat.setOnApplyWindowInsetsListener(activity.window.decorView) { _, insets ->
        currentInsets.value = insets
        insets
      }

    }
    statusBar.effect()
    navigationBar.effect()
    virtualKeyboard.effect()
    safeArea.effect()

    return this
  }

  companion object {
    init {
      QueryHelper.init() // 初始化
    }
  }
}


