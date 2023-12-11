package org.dweb_browser.shared

import org.dweb_browser.browser.web.IosInterface
import org.dweb_browser.browser.web.browserIosService
import org.dweb_browser.browser.web.registerIosIMP

public object DwebBrowserIosSupport {

  var browserService = browserIosService

  fun registerIosService(imp: IosInterface)  {
    registerIosIMP(imp)
  }
}
