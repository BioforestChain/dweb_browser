package org.dweb_browser.helper

import android.graphics.Rect
import androidx.core.graphics.Insets

fun Bounds.toAndroidXInsets() = Insets.of(
  left.toInt(),
  top.toInt(),
  right.toInt(),
  bottom.toInt(),
)

fun Bounds.toAndroidRect() = Rect(
  left.toInt(),
  top.toInt(),
  right.toInt(),
  bottom.toInt(),
)