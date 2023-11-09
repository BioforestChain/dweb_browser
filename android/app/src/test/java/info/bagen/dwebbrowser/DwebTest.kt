package info.bagen.dwebbrowser

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.ipc.helper.IpcHeaders
import org.dweb_browser.core.ipc.helper.IpcResponse
import org.dweb_browser.sys.boot.BootNMM
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.http.DwebHttpServerOptions
import org.dweb_browser.core.std.http.HttpNMM
import org.dweb_browser.core.std.http.createHttpDwebServer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DwebTest : AsyncBase() {

  class HttpTestNMM : NativeMicroModule("http.test.dweb", "http test") {
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

      val ioAsyncScope = MainScope() + ioAsyncExceptionHandler
      deferredList += ioAsyncScope.async(context = catcher) {
        for (i in 1..10) {
          delay(20)
          println("第 $i 次发送数据")
          val data = "/hi-$i"
          val res = nativeFetch(dwebServer.startResult.urlInfo.internal_origin + data)
          assertEquals("ECHO: $data", res.text())
        }
      }
      deferredList += ioAsyncScope.async(context = catcher) {
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