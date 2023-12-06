package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
actual fun SetSystemBarsColor(bgColor: Color, fgColor: Color) {
  // IOS 平台不需要实现，默认行为已经能自己适应
}