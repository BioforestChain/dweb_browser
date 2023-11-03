package org.dweb_browser.dwebview.ipcWeb

import kotlinx.cinterop.BetaInteropApi
import org.dweb_browser.dwebview.DWebMessagePort
import platform.Foundation.NSString
import platform.Foundation.create
import platform.darwin.NSObject

class WebMessage(val data: NSObject, val ports: List<DWebMessagePort>?) {
  constructor(data: NSObject) : this(data, listOf())

  companion object {
    @OptIn(BetaInteropApi::class)
    fun from(message: String, ports: List<DWebMessagePort>? = null) = WebMessage(NSString.create(string = message), ports)
  }
}