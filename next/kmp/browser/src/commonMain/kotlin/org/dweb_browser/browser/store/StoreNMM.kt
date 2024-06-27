package org.dweb_browser.browser.store

import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.onRenderer


class StoreNMM : NativeMicroModule("store.browser.dweb", "Store Manager") {

  init {
    short_name = BrowserI18nResource.Store.short_name.text
    categories = listOf(
      MICRO_MODULE_CATEGORY.Application,
      MICRO_MODULE_CATEGORY.Service,
      MICRO_MODULE_CATEGORY.Database_Service,
    )
    icons = listOf(
      ImageResource(
        src = "file:///sys/browser-icons/store.browser.dweb.svg",
        type = "image/svg+xml",
        // purpose = "monochrome"
      )
    )
  }

  inner class StoreRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    private val storeController = StoreController(this)

    override suspend fun _bootstrap() {
      routes(
        /** 打开渲染 */
        "/open" bind PureMethod.GET by defineBooleanResponse {
          return@defineBooleanResponse true
        },
      ).cors()


      onRenderer {
        getMainWindow().apply {
          setStateFromManifest(manifest)
          storeController.openRender(this)
        }
      }
    }

    override suspend fun _shutdown() {

    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = StoreRuntime(bootstrapContext)
}
