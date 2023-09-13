package info.bagen.dwebbrowser.microService.sys.device

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.http.bind
import org.dweb_browser.microservice.http.routes

class NfcNMM : NativeMicroModule("nfc.sys.dweb", "nfc") {

  init {
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service);
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      /** 开始读取当前信息*/
      "/read" bind HttpMethod.Get to definePureResponse {
        PureResponse(HttpStatusCode.OK)
      },
    )
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }
}