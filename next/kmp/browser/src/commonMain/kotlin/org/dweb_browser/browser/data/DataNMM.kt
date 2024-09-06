package org.dweb_browser.browser.data

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.onRenderer


class DataNMM : NativeMicroModule("data.browser.dweb", DataI18n.short_name.text) {

  init {
    short_name = DataI18n.short_name.text
    categories = listOf(
      MICRO_MODULE_CATEGORY.Service,
      MICRO_MODULE_CATEGORY.Database_Service,
    ).let {
      when {
        envSwitch.isEnabled(ENV_SWITCH_KEY.DATA_MANAGER_GUI) -> it + MICRO_MODULE_CATEGORY.Application
        else -> it
      }
    }
    icons = listOf(
      ImageResource(
        src = "file:///sys/browser-icons/data.browser.dweb.svg",
        type = "image/svg+xml",
        // purpose = "monochrome"
      )
    )
  }

  inner class DataRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    private val storeController = DataController(this)

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

  override fun createRuntime(bootstrapContext: BootstrapContext) = DataRuntime(bootstrapContext)
}
