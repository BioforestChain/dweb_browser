package org.dweb_browser.helper

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIEdgeInsetsMake


@OptIn(ExperimentalForeignApi::class)
fun PureBounds.toIosUIEdgeInsets() = UIEdgeInsetsMake(
  top = top.toDouble(),
  left = left.toDouble(),
  bottom = bottom.toDouble(),
  right = right.toDouble(),
)