package info.bagen.dwebbrowser.microService.sys.device

import info.bagen.dwebbrowser.microService.sys.device.model.LocationInfo
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import org.dweb_browser.helper.printDebug
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.http.PureStringBody
import org.dweb_browser.microservice.http.bind
import org.dweb_browser.microservice.http.routes

fun debugLocation(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("Location", tag, msg, err)

class LocationNMM : NativeMicroModule("location.sys.dweb", "location") {

  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Process_Service);
  }

  val locationInfo = LocationInfo()
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      /** 获取当前位置信息*/
      "/info" bind HttpMethod.Get to definePureResponse {
        val result = locationInfo.getLocationInfo()
        PureResponse(HttpStatusCode.OK, body = PureStringBody(result))
      },
    )
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }
}