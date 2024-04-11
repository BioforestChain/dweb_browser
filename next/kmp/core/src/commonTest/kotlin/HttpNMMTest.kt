package info.bagen.dwebbrowser

import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.http.DwebHttpServerOptions
import org.dweb_browser.core.std.http.HttpNMM
import org.dweb_browser.core.std.http.createHttpDwebServer
import org.dweb_browser.helper.addDebugTags
import org.dweb_browser.helper.collectIn
import org.dweb_browser.pure.http.PureChannel
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpNMMTest {
  init {
    addDebugTags(listOf("/.+/"))
  }

  @Test
  fun testListen() = runCommonTest(1000) {

    class TestMicroModule(mmid: String = "test.httpListen.dweb") :
      NativeMicroModule(mmid, "test Http Listen") {
      inner class TestRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
        override suspend fun _bootstrap() {
          val server = createHttpDwebServer(DwebHttpServerOptions(subdomain = "testwww"))
          val serverIpc = server.listen()
          serverIpc.onRequest("listen").collectIn(mmScope) { event ->
            val ipcServerRequest = event.consume()
            serverIpc.postResponse(ipcServerRequest.reqId, PureResponse.build {
              body(ipcServerRequest.url)
            })
          }
          println("http start at ${server.startResult.urlInfo}")
        }

        override suspend fun _shutdown() {
        }
      }

      override fun createRuntime(bootstrapContext: BootstrapContext) = TestRuntime(bootstrapContext)
    }


    val dns = DnsNMM()
    val httpMM = HttpNMM()
    dns.install(httpMM)

    val serverMM = TestMicroModule()
    dns.install(serverMM)
    val dnsRuntime = dns.bootstrap()
    val serverRuntime = dnsRuntime.open(serverMM.mmid) as TestMicroModule.TestRuntime;

    val url = "https://testwww.${serverRuntime.mmid}/hi~~"
    val res = serverRuntime.nativeFetch(url).text()
    assertEquals(url, res)
  }


  @Test
  fun testWebSocket() = runCommonTest(1000) {

    class TestMicroModule(mmid: String = "test.httpListen.dweb") :
      NativeMicroModule(mmid, "test Http Listen") {
      inner class TestRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
        override suspend fun _bootstrap() {
          val server = createHttpDwebServer(DwebHttpServerOptions(subdomain = "testwww"))
          val serverIpc = server.listen()
          serverIpc.onRequest("listen").collectIn(mmScope) { event ->
            val ipcServerRequest = event.consume()
            println("QAQ onRequest=$ipcServerRequest")
            val response = nativeFetch(ipcServerRequest.toPure().toClient())
            println("QAQ response=$response")
            serverIpc.postResponse(ipcServerRequest.reqId, response)
          }
          println("http start at ${server.startResult.urlInfo}")

          routes("/ws" byChannel { ctx ->
            for (i in 1..10) {
              ctx.sendText("hi~$i")
            }
            ctx.close()
          })
        }

        override suspend fun _shutdown() {
        }
      }

      override fun createRuntime(bootstrapContext: BootstrapContext) = TestRuntime(bootstrapContext)
    }


    val dns = DnsNMM()
    val httpMM = HttpNMM()
    dns.install(httpMM)

    val serverMM = TestMicroModule()
    dns.install(serverMM)
    val dnsRuntime = dns.bootstrap()
    val serverRuntime = dnsRuntime.open(serverMM.mmid) as TestMicroModule.TestRuntime;

    val channelDeferred = CompletableDeferred(PureChannel())
    serverRuntime.nativeFetch(
      PureClientRequest(
        href = "https://testwww.${serverRuntime.mmid}/ws",
        method = PureMethod.GET,
        channel = channelDeferred,
      )
    )
    val channel = channelDeferred.await()
    val ctx = channel.start()
    var res = ""
    for (i in ctx.income) {
      println("QAQ $i")
      res = i.text
    }
    println("DONE")
    assertEquals("hi~10", res)
  }
}