package org.dweb_browser.helper.platform

import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import java.awt.Container
import java.awt.DisplayMode

private const val animationFrameMinMs = 8 // 120pfs
private const val animationFrameDefaultMs = 16 // 60fps
private const val animationFrameMaxMs = 100 // 10pfs

private val cache = WeakHashMap<DisplayMode, Int>()
fun DisplayMode.getAnimationFrameMs(): Int = cache.getOrPut(this) {
  return if (refreshRate == DisplayMode.REFRESH_RATE_UNKNOWN) {
    animationFrameDefaultMs
  } else {
    (1000 / refreshRate)//
      .coerceAtMost(animationFrameMaxMs).coerceAtLeast(animationFrameMinMs)
  }
}

fun Container.getAnimationFrameMs() = graphicsConfiguration.device.displayMode.getAnimationFrameMs()
