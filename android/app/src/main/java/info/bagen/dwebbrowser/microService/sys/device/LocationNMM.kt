package info.bagen.dwebbrowser.microService.sys.device

import org.dweb_browser.helper.*
import info.bagen.dwebbrowser.microService.sys.device.model.LocationInfo
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes

fun debugLocation(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("Location", tag, msg, err)

class LocationNMM: NativeMicroModule("location.sys.dweb","location") {

  override val categories = mutableListOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Process_Service);

    val locationInfo = LocationInfo()
    override suspend fun _bootstrap(bootstrapContext: BootstrapContext)
 {
        apiRouting = routes(
            /** 获取当前位置信息*/
            "/info" bind Method.GET to defineHandler { request ->
                val result = locationInfo.getLocationInfo()
                Response(Status.OK).body(result)
            },
        )
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}