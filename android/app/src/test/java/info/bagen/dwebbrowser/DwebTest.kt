package info.bagen.dwebbrowser

import org.dweb_browser.microservice.ipc.helper.IpcHeaders
import org.dweb_browser.microservice.ipc.helper.IpcResponse
import org.dweb_browser.microservice.sys.boot.BootNMM
import org.dweb_browser.microservice.sys.dns.DnsNMM
import org.dweb_browser.microservice.sys.dns.nativeFetch
import org.dweb_browser.microservice.sys.http.DwebHttpServerOptions
import org.dweb_browser.microservice.sys.http.HttpNMM
import org.dweb_browser.microservice.sys.http.createHttpDwebServer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.text
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DwebTest : AsyncBase() {

    class HttpTestNMM : NativeMicroModule("http.test.dweb","http test") {
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
                    println("第 $i 次发送数据")
                    val data = "/hi-$i"
                    val res = nativeFetch(dwebServer.startResult.urlInfo.internal_origin + data)
                    assertEquals("ECHO: $data", res.text())
                }
            }
            deferredList += GlobalScope.async(context = catcher) {
                for (i in 1..10) {
                    delay(20)
                    println("第 $i 次发送数据")
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

        /// 安装系统应用
        val httpNMM = HttpNMM().also { dnsNMM.install(it) }

        /// 安装测试应用
        val httpTestNMM = HttpTestNMM().also { dnsNMM.install(it) }

        /// 安装启动程序
        val bootNMM = BootNMM(listOf(httpTestNMM.mmid)).also { dnsNMM.install(it) }

        /// 启动
        dnsNMM.bootstrap()


        prepareReady.await()
        for (def in deferredList) {
            def.await()
        }
    }
}