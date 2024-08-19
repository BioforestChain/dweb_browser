package org.dweb_browser.helper

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIEdgeInsets
import platform.UIKit.UIEdgeInsetsMake


@OptIn(ExperimentalForeignApi::class)
public fun PureBounds.toIosUIEdgeInsets(): CValue<UIEdgeInsets> = UIEdgeInsetsMake(
  top = top.toDouble(),
  left = left.toDouble(),
  bottom = bottom.toDouble(),
  right = right.toDouble(),
)