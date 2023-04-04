package info.bagen.rust.plaoc.microService.sys.plugin.dwebServiceWorker

import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.service.DownLoadController
import info.bagen.rust.plaoc.util.DwebBrowserUtil
import org.http4k.core.Method
import org.http4k.routing.bind
import org.http4k.routing.routes

class UpdateControllerNMM:NativeMicroModule("update-controller.sys.dweb") {
    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        apiRouting = routes(
            "/pause" bind Method.GET to defineHandler { _, ipc ->
                DwebBrowserUtil.INSTANCE.mBinderService?.invokeUpdateDownloadStatus(
                    ipc.remote.mmid, DownLoadController.PAUSE
                )
            },
            "/resume" bind Method.GET to defineHandler { _, ipc ->
                DwebBrowserUtil.INSTANCE.mBinderService?.invokeUpdateDownloadStatus(
                    ipc.remote.mmid, DownLoadController.RESUME
                )
            },
            "/cancel" bind Method.GET to defineHandler { _, ipc ->
                DwebBrowserUtil.INSTANCE.mBinderService?.invokeUpdateDownloadStatus(
                    ipc.remote.mmid, DownLoadController.CANCEL
                )
            }
        )
    }

    override suspend fun _shutdown() {
    }
}