package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.UIScreen

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberScreenSize(): ScreenSize {
  val density = LocalDensity.current.density
  return remember {
    UIScreen.mainScreen.bounds.useContents {
      ScreenSize((size.width * density).toInt(), (size.height * density).toInt())
    }
  }
}