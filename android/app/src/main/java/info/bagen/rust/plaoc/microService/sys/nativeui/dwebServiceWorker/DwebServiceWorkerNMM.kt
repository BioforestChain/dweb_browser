package info.bagen.rust.plaoc.microService.sys.nativeui.dwebServiceWorker

import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import org.http4k.core.Method
import org.http4k.routing.bind
import org.http4k.routing.routes

class DwebServiceWorkerNMM:NativeMicroModule("service-worker.nativeui.sys.dweb") {

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        apiRouting = routes(
            "/close" bind Method.GET to defineHandler { _, ipc ->
                ipc.close()
            },
            "/restart" bind Method.GET to defineHandler { request, ipc ->

            }
        )
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}