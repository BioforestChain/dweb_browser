package org.dweb_browser.helper.platform.ios

import org.dweb_browser.helper.platform.ios_browser.*

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
public fun doModuleDwebBrowser() {
  val broser = DwebWebBrowser()
  broser.sayHello()
  broser.infoSelf()
//  broser.infoSelf1()
  println("doModuleDwebBrowser")

  val ss = DwebWKWebView()
  println("DwebWKWebView")

}