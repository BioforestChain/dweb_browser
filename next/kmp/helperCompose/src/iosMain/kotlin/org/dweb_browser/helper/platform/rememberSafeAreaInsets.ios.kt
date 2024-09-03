package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.uikit.InterfaceOrientation
import org.dweb_browser.helper.PureBounds
import kotlin.math.max

@OptIn(InternalComposeUiApi::class)
@Composable
actual fun rememberSafeAreaInsets(): PureBounds {
  val safeBounds = rememberSafeAreaInsetsCommon()
  LocalPureViewController.current.asIosPureViewController().uiViewControllerInMain
  val orientation = rememberInterfaceOrientation()
  return when (orientation) {
    InterfaceOrientation.Portrait, InterfaceOrientation.PortraitUpsideDown -> safeBounds
    /// 顶部是状态栏的呼出空间
    InterfaceOrientation.LandscapeLeft -> safeBounds.copy(
      top = max(safeBounds.top, safeBounds.bottom),
      left = 0f,
    )

    InterfaceOrientation.LandscapeRight -> safeBounds.copy(
      top = max(safeBounds.top, safeBounds.bottom),
      right = 0f,
    )
  }
}


