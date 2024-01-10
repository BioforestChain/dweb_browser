package org.dweb_browser.sys.microphone

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.pure.http.PureMethod

val debugMicroPhone = Debugger("MicroPhone")

class MicroPhoneNMM : NativeMicroModule("microphone.sys.dweb", "camera") {
  private val microPhoneManage = MicroPhoneManage()

  init {
    categories = listOf(
      MICRO_MODULE_CATEGORY.Service,
      MICRO_MODULE_CATEGORY.Process_Service
    )
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      "/recordSound" bind PureMethod.GET by defineEmptyResponse {
        debugMicroPhone("recordSound", "enter")
        val fromMM = bootstrapContext.dns.query(ipc.remote.mmid) ?: this@MicroPhoneNMM
        microPhoneManage.recordSound(fromMM)
      },
    )
  }

  override suspend fun _shutdown() {
  }
}