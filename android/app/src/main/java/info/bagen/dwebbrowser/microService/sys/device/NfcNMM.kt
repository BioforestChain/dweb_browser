package info.bagen.dwebbrowser.microService.sys.device

import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes

class NfcNMM : NativeMicroModule("nfc.sys.dweb", "nfc") {

  init {
    categories =
      mutableListOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service);
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    apiRouting = routes(
      /** 开始读取当前信息*/
      "/read" bind Method.GET to defineHandler { request ->
        Response(Status.OK)
      },
    )
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }
}