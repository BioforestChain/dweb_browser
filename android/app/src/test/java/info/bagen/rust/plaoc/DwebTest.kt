package info.bagen.rust.plaoc

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
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DwebTest : AsyncBase() {
    companion object {
        var deferredList = mutableListOf<Deferred<Unit>>()
    }

    class HttpTestNMM : NativeMicroModule("http.test.dweb") {
        override suspend fun _bootstrap() {
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

            val job = GlobalScope.launch {
                for (i in 1..100) {
                    delay(1000)
                    println("第 $i 次发送数据")
                    val data = "/hi-$i"
                    val res = nativeFetch(dwebServer.startResult.urlInfo.internal_origin + data)
                    assertEquals("ECHO: $data", res.text())
                }
            }
            deferredList += CompletableDeferred(job)
        }

        override suspend fun _shutdown() {
        }
    }

    @Test
    fun testHttp() = runBlocking {
        val dnsNMM = DnsNMM()

        /// 安装系统应用
        val httpNMM = HttpNMM().also { dnsNMM.install(it) }

        /// 安装测试应用
        val httpTestNMM = HttpTestNMM().also { dnsNMM.install(it) }

        /// 安装启动程序
        val bootNMM = BootNMM(listOf(httpTestNMM.mmid)).also { dnsNMM.install(it) }

        /// 启动
        dnsNMM.bootstrap()


        deferredList += CompletableDeferred(launch {
            delay(10000)
        })
//        deferredList.awaitAll()
//        println(deferredList.size)
//        deferredList.awaitAll()
    }
}