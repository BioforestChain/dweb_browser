package org.dweb_browser.sys.window.render

import android.view.WindowManager
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.platform.InsetsController
import org.dweb_browser.helper.platform.asAndroid
import org.dweb_browser.helper.platform.rememberInsetsController
import org.dweb_browser.helper.platform.rememberPureViewBox
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowsManager
import org.dweb_browser.sys.window.core.WindowsManagerState.Companion.watchedState
import org.dweb_browser.sys.window.core.constant.debugWindow
import org.dweb_browser.sys.window.helper.LocalWindowsImeVisible

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T : WindowController> WindowsManager<T>.EffectKeyboard() {
  val imeVisible = LocalWindowsImeVisible.current.value
  val ime = WindowInsets.imeAnimationTarget // 直接使用ime，数据不稳定，会变化，改为imeAnimationTarget就是固定值
  val density = LocalDensity.current
  val view = LocalView.current

  LaunchedEffect(view, imeVisible) { // WindowInsets.ime 对象并不会变化，所以导致这个重组不会重复执行
    state.imeVisible = imeVisible // 由于小米手机的安全键盘存在问题，会出现WindowInsets.isImeVisible不正确的情况
    state.imeBoundingRect = if (imeVisible) {
      val imeHeightDp = ime.getBottom(density) / density.density
      PureRect(
        x = 0f,
        y = view.height / density.density - imeHeightDp,
        height = imeHeightDp,
        width = view.width / density.density,
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
  rememberInsetsController()?.apply {
    val hasWin = winListFlow.collectAsState().value.isNotEmpty()
    // TODO 如果有最大化窗口，根据当前窗口改变系统导航栏按钮的颜色
    LaunchedEffect(hasWin) {
      navigationBarsBehavior = when {
        hasWin -> {
          hideNavigationBars() // 立刻隐藏导航栏
          InsetsController.NavigationBarsBehavior.ShowBySwipe
        }

        else -> InsetsController.NavigationBarsBehavior.Show
      }
    }
    Effect()
    if (false) {
      DebugDemo()
    }
  }
}

@Composable
private fun <T : WindowController> WindowsManager<T>.EffectSafeModel() {
  val window = rememberPureViewBox().asAndroid().activity.window
  val safeMode by watchedState { safeMode }
  if (safeMode) {
    DisposableEffect(window) {
      window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
      onDispose {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
      }
    }
  }
}

@Composable
actual fun <T : WindowController> WindowsManager<T>.WindowsManagerEffect() {
  /// 键盘的互操作性
  EffectKeyboard()
  /// 底部导航栏的互操作 关闭隐藏虚拟导航栏
//  EffectNavigationBar()
  /// 窗口截屏安全限制
  EffectSafeModel()
}