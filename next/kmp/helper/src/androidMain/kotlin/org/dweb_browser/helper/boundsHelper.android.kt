package org.dweb_browser.helper

import android.graphics.Rect
import androidx.core.graphics.Insets

public fun PureBounds.toAndroidXInsets(): Insets = Insets.of(
  left.toInt(),
  top.toInt(),
  right.toInt(),
  bottom.toInt(),
)

public fun PureBounds.toAndroidRect(): Rect = Rect(
  left.toInt(),
  top.toInt(),
  right.toInt(),
  bottom.toInt(),
)