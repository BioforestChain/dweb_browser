package info.bagen.rust.plaoc

import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.text
import info.bagen.rust.plaoc.microService.ipc.IpcHeaders
import info.bagen.rust.plaoc.microService.ipc.IpcResponse
import info.bagen.rust.plaoc.microService.sys.boot.BootNMM
import info.bagen.rust.plaoc.microService.sys.dns.DnsNMM
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import info.bagen.rust.plaoc.microService.sys.http.DwebHttpServerOptions
import info.bagen.rust.plaoc.microService.sys.http.HttpNMM
import info.bagen.rust.plaoc.microService.sys.http.createHttpDwebServer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DwebTest : AsyncBase() {

    class HttpTestNMM : NativeMicroModule("http.test.dweb") {
        override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
            val dwebServer = createHttpDwebServer(DwebHttpServerOptions());
            dwebServer.listen().onRequest { (request, ipc) ->
                ipc.postMessage(
                    IpcResponse.fromText(
                        request.req_id,
                        200,
                        IpcHeaders(),
                        "ECHO: " + request.url,
                        ipc
                    )
                )
            }
            _afterShutdownSignal.listen { dwebServer.close() }


            val internalServer =
                createHttpDwebServer(DwebHttpServerOptions(subdomain = "internal"));
            internalServer.listen().onRequest { (request, ipc) ->
                ipc.postMessage(
                    IpcResponse.fromText(
                        request.req_id,
                        200,
                        IpcHeaders(),
                        "ECHO/INTERNAL: " + request.url,
                        ipc
                    )
                )
            }
            _afterShutdownSignal.listen { dwebServer.close() }

            deferredList += GlobalScope.async(context = catcher) {
                for (i in 1..10) {
                    delay(20)
                    println("??? $i ???????????????")
                    val data = "/hi-$i"
                    val res = nativeFetch(dwebServer.startResult.urlInfo.internal_origin + data)
                    assertEquals("ECHO: $data", res.text())
                }
            }
            deferredList += GlobalScope.async(context = catcher) {
                for (i in 1..10) {
                    delay(20)
                    println("??? $i ???????????????")
                    val data = "/hi-$i"
                    val res = nativeFetch(internalServer.startResult.urlInfo.internal_origin + data)
                    assertEquals("ECHO/INTERNAL: $data", res.text())
                }
            }
            prepareReady.complete(Unit)
        }

        override suspend fun _shutdown() {
        }
    }

    @Test
    fun testHttp() = runBlocking {
        enableDwebDebug(listOf("stream-ipc"))

        val dnsNMM = DnsNMM()

        /// ??????????????????
        val httpNMM = HttpNMM().also { dnsNMM.install(it) }

        /// ??????????????????
        val httpTestNMM = HttpTestNMM().also { dnsNMM.install(it) }

        /// ??????????????????
        val bootNMM = BootNMM(listOf(httpTestNMM.mmid)).also { dnsNMM.install(it) }

        /// ??????
        dnsNMM.bootstrap()


        prepareReady.await()
        for (def in deferredList) {
            def.await()
        }
    }
}