package org.dweb_browser.window.render


import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch
import org.dweb_browser.window.core.WindowBounds
import org.dweb_browser.window.core.WindowController
import org.dweb_browser.window.core.WindowsManager
import org.dweb_browser.window.core.constant.debugWindow


@OptIn(ExperimentalLayoutApi::class)
@Composable
actual fun <T : WindowController> WindowsManager<T>.EffectKeyboard() {

  val imeVisible = WindowInsets.isImeVisible
  state.imeVisible = imeVisible
  val ime = WindowInsets.imeAnimationTarget // 直接使用ime，数据不稳定，会变化，改为imeAnimationTarget就是固定值
  val density = LocalDensity.current
  val view = LocalView.current

  LaunchedEffect(imeVisible) { // WindowInsets.ime 对象并不会变化，所以导致这个重组不会重复执行
    state.imeBounds = if (imeVisible) {
      val imeHeightDp = ime.getBottom(density) / density.density
      WindowBounds(
        left = 0f,
        top = view.height / density.density - imeHeightDp,
        height = imeHeightDp,
        width = view.width / density.density,
      )
    } else {
      WindowBounds.Zero
    }

    // 输入法高度即为 heightDiff
    debugWindow("ManagerState/IME", "imeBounds:${state.imeBounds}, imeVisible:${state.imeVisible}")
  }

}

@Composable
actual fun <T : WindowController> WindowsManager<T>.EffectNavigationBar() {
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
actual fun WindowController.BackHandler(enabled: Boolean, onBack: () -> Unit) {
  androidx.activity.compose.BackHandler(enabled, onBack)
}
