package org.dweb_browser.browser.link

import org.dweb_browser.helper.printDebug
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.sys.download.db.DeskWebLink

fun debugWebLink(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("link", tag, msg, err)

class WebLinkMicroModule(webLink: DeskWebLink) : NativeMicroModule(webLink.id, webLink.url) {
  init {
    short_name = webLink.title.substring(0, minOf(5, webLink.title.length))
    categories = listOf(MICRO_MODULE_CATEGORY.Application, MICRO_MODULE_CATEGORY.Web_Browser)
    icons = listOf(webLink.icon)
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    /// TODO 在浏览器中打开新的Tab页面，或者如果能找到url完全一样的，就直接聚焦那个页面
  }

  override suspend fun _shutdown() {
  }
}