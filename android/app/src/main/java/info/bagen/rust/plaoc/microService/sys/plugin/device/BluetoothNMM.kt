package info.bagen.rust.plaoc.microService.sys.plugin.device

import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes

class BluetoothNMM:NativeMicroModule("bluetooth.sys.dweb") {
    override suspend fun _bootstrap() {
        apiRouting = routes(
            /** 打开蓝牙*/
            "/open" bind Method.GET to defineHandler { request ->
                Response(Status.OK)
            },
            /** 关闭蓝牙*/
            "/close" bind Method.GET to defineHandler { request ->
                Response(Status.OK)
            },
        )
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}