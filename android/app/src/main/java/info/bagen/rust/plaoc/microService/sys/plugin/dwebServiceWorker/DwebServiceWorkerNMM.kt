package info.bagen.rust.plaoc.microService.sys.plugin.dwebServiceWorker

import info.bagen.rust.plaoc.microService.core.AndroidNativeMicroModule
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.helper.Mmid
import org.http4k.core.Method
import org.http4k.routing.bind
import org.http4k.routing.routes

class DwebServiceWorkerNMM:AndroidNativeMicroModule("service-worker.sys.dweb") {
    override fun openActivity(remoteMmid: Mmid) {
        TODO("Not yet implemented")
    }

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