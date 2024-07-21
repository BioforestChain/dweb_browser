package org.dweb_browser.helper

import android.graphics.Rect
import androidx.core.graphics.Insets

fun PureBounds.toAndroidXInsets() = Insets.of(
  left.toInt(),
  top.toInt(),
  right.toInt(),
  bottom.toInt(),
)

fun PureBounds.toAndroidRect() = Rect(
  left.toInt(),
  top.toInt(),
  right.toInt(),
  bottom.toInt(),
)