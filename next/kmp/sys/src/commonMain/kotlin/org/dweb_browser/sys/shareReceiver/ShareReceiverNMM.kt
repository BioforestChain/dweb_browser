package org.dweb_browser.sys.shareReceiver

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.sys.window.ext.onRenderer


val debugShareReceiver = Debugger("shareReceiver")

class ShareReceiverNMM :
  NativeMicroModule("share-receiver.sys.dweb", ShareReceiverI18nResource.name.text) {
  init {
    short_name = ShareReceiverI18nResource.shortName.text
    categories = listOf(
      MICRO_MODULE_CATEGORY.Service
    )
  }

  inner class ShareReceiverRuntime(override val bootstrapContext: BootstrapContext) :
    NativeRuntime() {
    override suspend fun _bootstrap() {
//      routes(
//        "/share" bind PureMethod.POST by defineBooleanResponse {
//          true
//        }
//      ).cors()
//      onRenderer {
//
//      }
    }

    override suspend fun _shutdown() {}
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) =
    ShareReceiverRuntime(bootstrapContext)
}