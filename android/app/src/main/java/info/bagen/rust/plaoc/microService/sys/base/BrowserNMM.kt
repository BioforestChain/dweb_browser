package info.bagen.rust.plaoc.microService.sys.base

import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.openHomeActivity


class BrowserNMM:NativeMicroModule("browser.sys.dweb") {
    override suspend fun _bootstrap() {
        openHomeActivity()
//        apiRouting = routes(
//            "/open" bind Method.GET to defineHandler { request ->
//                println("DNS#apiRouting===>$mmid  ${request.uri.path}")
//                openHomeActivity()
//                Response(Status.OK)
//            },
//            "/close" bind Method.GET to defineHandler { request ->
//                println("DNS#apiRouting===>$mmid  ${request.uri.path}")
//                Response(Status.OK)
//            },
//        )
    }

    override suspend fun _shutdown() {
//        nativeFetch()
    }
}