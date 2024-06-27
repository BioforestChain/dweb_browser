package org.dweb_browser.sys.about

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.UUID
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.onRenderer

class AboutNMM : NativeMicroModule("about.sys.dweb", "About") {
  init {
    short_name = "About"
    categories = listOf(
      MICRO_MODULE_CATEGORY.Application
    )
    icons = listOf(
      ImageResource(src = "file:///sys/browser-icons/$mmid.svg", type = "image/svg+xml")
    )
  }

  inner class AboutRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    override suspend fun _bootstrap() {
      onRenderer {
        getMainWindow().apply {
          setStateFromManifest(manifest)
          openAboutPage(id)
        }
      }
    }

    override suspend fun _shutdown() {}
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = AboutRuntime(bootstrapContext)
}

expect suspend fun AboutNMM.AboutRuntime.openAboutPage(id: UUID)

