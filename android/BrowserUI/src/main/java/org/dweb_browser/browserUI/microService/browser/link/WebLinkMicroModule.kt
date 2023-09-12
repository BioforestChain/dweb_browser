package org.dweb_browser.browserUI.microService.browser.link

import org.dweb_browser.browserUI.database.DeskWebLink
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.printDebug
import org.dweb_browser.microservice.core.AndroidNativeMicroModule
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY

fun debugWebLink(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("link", tag, msg, err)

class WebLinkMicroModule(webLink: DeskWebLink) : AndroidNativeMicroModule(webLink.id, "Web Link") {
  init {
    short_name = "WebLink"
    categories = mutableListOf(MICRO_MODULE_CATEGORY.Application, MICRO_MODULE_CATEGORY.Web_Browser)
    icons += listOf(ImageResource(src = "file:///sys/browser/web/logo.svg"))
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
  }

  override suspend fun _shutdown() {
  }
}