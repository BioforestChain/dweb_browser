package org.dweb_browser.browser.web

import org.dweb_browser.browser.web.data.WebLinkManifest
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule

/**
 * 仅用于添加到桌面后，能够点击打开 web
 */
class WebLinkMicroModule(webLink: WebLinkManifest) : NativeMicroModule(webLink.id, webLink.title) {
  init {
    short_name = webLink.title
    categories = listOf(MICRO_MODULE_CATEGORY.Application, MICRO_MODULE_CATEGORY.Web_Browser)
    icons = webLink.icons
    homepage_url = webLink.url
  }

  inner class WebLinkRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    override suspend fun _bootstrap() {
      /// TODO 在浏览器中打开新的Tab页面，或者如果能找到url完全一样的，就直接聚焦那个页面
    }

    override suspend fun _shutdown() {
    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = WebLinkRuntime(bootstrapContext)
}