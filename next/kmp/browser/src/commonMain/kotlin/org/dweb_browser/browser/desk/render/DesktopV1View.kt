package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlinx.coroutines.launch
import org.dweb_browser.browser.desk.DesktopV1Controller
import org.dweb_browser.dwebview.Render
import org.dweb_browser.helper.PureBounds
import org.dweb_browser.helper.compose.NativeBackHandler

@Composable
fun DesktopV1Controller.RenderImpl() {
  val safeContent = WindowInsets.safeContent
  val density = LocalDensity.current
  val layoutDirection = LocalLayoutDirection.current
  LaunchedEffect(safeContent, density, layoutDirection) {
    desktopView.setSafeAreaInset(
      PureBounds(
        left = safeContent.getLeft(density, layoutDirection) / density.density,
        top = safeContent.getTop(density) / density.density,
        right = safeContent.getRight(density, layoutDirection) / density.density,
        bottom = safeContent.getBottom(density) / density.density,
      )
    )
  }
  desktopView.Render(Modifier.fillMaxSize())
  val canGoBack by desktopView.canGoBackStateFlow.collectAsState()
  NativeBackHandler(canGoBack) {
    desktopView.lifecycleScope.launch {
      desktopView.goBack()
    }
  }

  /// 新版本
  // newVersionController.NewVersionView() // 先不搞这个
}


