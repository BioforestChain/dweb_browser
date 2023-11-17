package org.dweb_browser.helper.platform.ios

import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun add(a: Int, b: Int) {
  IosKit()
  IosKit2().addWithA(1, 2)
  IosKit3().addWithA(1, 2, 3, 4, 5)
}