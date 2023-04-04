package info.bagen.rust.plaoc.microService.sys.plugin.dwebServiceWorker

import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule

class DwebServiceWorkerNMM:NativeMicroModule("dweb-service-worker.sys.dweb") {
    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        TODO("Not yet implemented")
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}