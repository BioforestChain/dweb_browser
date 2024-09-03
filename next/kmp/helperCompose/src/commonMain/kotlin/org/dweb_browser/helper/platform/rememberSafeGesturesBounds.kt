package org.dweb_browser.helper.platform

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import org.dweb_browser.helper.PureBounds
import org.dweb_browser.helper.compose.asPureBounds

@Composable
expect fun rememberSafeAreaInsets(): PureBounds

@Composable
internal fun rememberSafeAreaInsetsCommon(): PureBounds {
  /// 不包含 WindowInsets.waterfall，这是曲面屏的曲面部分
  return WindowInsets.systemBars.union(WindowInsets.displayCutout).asPureBounds()
}