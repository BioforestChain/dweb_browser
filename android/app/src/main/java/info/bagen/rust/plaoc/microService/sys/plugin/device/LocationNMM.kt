package info.bagen.rust.plaoc.microService.sys.plugin.device

import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.sys.plugin.device.model.LocationInfo
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes

class LocationNMM:NativeMicroModule("location.sys.dweb") {

    val locationInfo = LocationInfo()
    override suspend fun _bootstrap() {
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