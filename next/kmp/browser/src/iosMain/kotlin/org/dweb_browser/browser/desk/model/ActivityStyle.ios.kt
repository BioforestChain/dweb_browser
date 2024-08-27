package org.dweb_browser.browser.desk.model

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.Composable
import org.dweb_browser.helper.platform.rememberDisplaySize
import org.dweb_browser.sys.device.model.DeviceInfo

@Composable
actual fun rememberActivityStyle(): ActivityStyle {
  val displaySize = rememberDisplaySize()
  val deviceType = DeviceInfo.deviceName
  println("QAQ deviceType=$deviceType")
  /**
   * iPhone 11 Pro cutoutOrStatusBarTop=44
   * iPhone 12 cutoutOrStatusBarTop=47
   * iPhone 13 mini cutoutOrStatusBarTop=50
   * iPhone 14 Pro cutoutOrStatusBarTop=59
   * iPhone 15 Pro cutoutOrStatusBarTop=59
   */
  val cutoutOrStatusBarTop = ActivityStyle.defaultCutoutOrStatusBarTop
  val cutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding().value
  println("QAQ cutoutOrStatusBarTop=$cutoutOrStatusBarTop cutoutTop=$cutoutTop")

  val openRadius = cutoutTop
  val canOverlayCutoutHeight = cutoutOrStatusBarTop - (10f / 59f * cutoutTop)
  when (deviceType) {
    /// 可能需要区分 灵动岛 和 刘海屏
    "iPhone 14 Pro",
    "iPhone 14 Pro Max",
    "iPhone 15",
    "iPhone 15 Plus",
    "iPhone 15 Pro",
    "iPhone 15 Pro Max",
    -> {
    }

    else -> {}
  }


  return ActivityStyle.common(
    displayWidth = displaySize.width,
    radius = 24f,
    openRadius = openRadius,
    topPadding = 0f,
    // 因为IOS使用原生的图层，所以禁用阴影
    shadowElevation = 0f,
    openShadowElevation = 0f,
    cutoutOrStatusBarTop = cutoutOrStatusBarTop,
    canOverlayCutoutHeight = canOverlayCutoutHeight,
  )
}