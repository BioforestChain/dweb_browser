package org.dweb_browser.browser.about

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.onRenderer

class AboutNMM : NativeMicroModule("about.browser.dweb", "About") {
  init {
    name = AboutI18nResource.shortName.text
    short_name = AboutI18nResource.shortName.text
    categories = listOf(
      MICRO_MODULE_CATEGORY.Application
    )
    icons = listOf(
      ImageResource(src = "file:///sys/browser-icons/$mmid.svg", type = "image/svg+xml")
    )

    val brandMap = ENV_SWITCH_KEY.entries.mapNotNull { it.experimental }.associate { brandData ->
      brandData.brand to brandData.disableVersion
    }.toMutableMap()
    ENV_SWITCH_KEY.entries.forEach { switchKey ->
      val brandData = switchKey.experimental ?: return@forEach
      if (envSwitch.isEnabled(switchKey)) {
        brandMap[brandData.brand] = brandData.enableVersion
      }
    }
    brandMap.forEach { (brand, version) ->
      IDWebView.Companion.brands.add(IDWebView.UserAgentBrandData(brand, version))
    }
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

