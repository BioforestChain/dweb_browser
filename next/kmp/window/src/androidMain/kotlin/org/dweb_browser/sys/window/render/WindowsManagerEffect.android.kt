package org.dweb_browser.sys.window.render

import android.view.WindowManager
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.platform.LocalPureViewBox
import org.dweb_browser.helper.platform.asAndroid
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowsManager
import org.dweb_browser.sys.window.core.WindowsManagerState.Companion.watchedState
import org.dweb_browser.sys.window.core.constant.debugWindow

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T : WindowController> WindowsManager<T>.EffectKeyboard() {
  state.imeVisible =
    LocalWindowsImeVisible.current.value // 由于小米手机的安全键盘存在问题，会出现WindowInsets.isImeVisible不正确的情况
  val ime = WindowInsets.imeAnimationTarget // 直接使用ime，数据不稳定，会变化，改为imeAnimationTarget就是固定值
  val density = LocalDensity.current
  val view = LocalView.current

  LaunchedEffect(view, state.imeVisible) { // WindowInsets.ime 对象并不会变化，所以导致这个重组不会重复执行
    state.imeBoundingRect = if (state.imeVisible) {
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
  val systemUiController = rememberSystemUiController()
  val scope = rememberCoroutineScope()
  DisposableEffect(hasMaximizedWins) {
    systemUiController.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

    debugWindow("WindowsManager.Render", "start watch maximize")
    val off = hasMaximizedWins.onChange {
      val hasMaximized = it.size > 0;
      /// 如果有窗口处于全屏模式，将操作系统的导航栏标记为隐藏，反之显示
      systemUiController.isNavigationBarVisible = !hasMaximized
      debugWindow("hasMaximized", hasMaximized)
    }
    scope.launch {
      off.emitSelf(hasMaximizedWins)
    }
    onDispose {
      debugWindow("WindowsManager.Render", "stop watch maximize")
      off()
    }
  }
}

@Composable
private fun <T : WindowController> WindowsManager<T>.EffectSafeModel() {
  val window = LocalPureViewBox.current.asAndroid().activity.window
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
  /// 底部导航栏的互操作
  EffectNavigationBar()
  /// 窗口截屏安全限制
  EffectSafeModel()
}