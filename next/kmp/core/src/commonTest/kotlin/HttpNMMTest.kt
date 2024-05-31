package info.bagen.dwebbrowser

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
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
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class HttpNMMTest {
  init {
    addDebugTags(listOf(":all:/.+/"))
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

  class TestMicroModule(mmid: String = "test.httpListen.dweb") :
    NativeMicroModule(mmid, "test Http Listen") {
    val MAX = 2

    inner class TestRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
      override suspend fun _bootstrap() {
        if (mmid.contains("server")) {

          val server = createHttpDwebServer(DwebHttpServerOptions(subdomain = "www"))
          val serverIpc = server.listen()
          serverIpc.onRequest("listen").collectIn(mmScope) { event ->
            val ipcServerRequest = event.consume()
            println("QWQ GG onRequest=$ipcServerRequest")
            val pureClientRequest = ipcServerRequest.toPure().toClient()
              .run { copy(href = href.replace(Regex("https://[^/]+"), "file://$mmid")) }
            println("QWQ GG pureClientRequest=$pureClientRequest")
            val response = nativeFetch(pureClientRequest)
            println("QWQ GG response=$response")
            serverIpc.postResponse(ipcServerRequest.reqId, response)
          }
          println("http start at ${server.startResult.urlInfo}")

          routes("/ws" byChannel { ctx ->
            for (i in 1..MAX) {
              ctx.sendText("hi~$i")
            }
            println("QWQ server ctx.close")
            ctx.close()
          })
        }
      }

      override suspend fun _shutdown() {
      }
    }

    override fun createRuntime(bootstrapContext: BootstrapContext) = TestRuntime(bootstrapContext)
  }

  val webSocketTester: suspend CoroutineScope.(Int) -> Unit = { time ->
    println("---test-$time")


    val dns = DnsNMM()
    val httpMM = HttpNMM()
    dns.install(httpMM)

    val serverMM = TestMicroModule("server.dweb")
    val clientMM = TestMicroModule("client.dweb")
    dns.install(serverMM)
    dns.install(clientMM)
    val dnsRuntime = dns.bootstrap()
    val serverRuntime = dnsRuntime.open(serverMM.mmid) as TestMicroModule.TestRuntime;
    val clientRuntime = dnsRuntime.open(clientMM.mmid) as TestMicroModule.TestRuntime;

    for (j in 1..20) {
      println("---sub-test-$time.$j")
      val channelDeferred = CompletableDeferred(PureChannel())
      clientRuntime.nativeFetch(
        PureClientRequest(
          href = "https://www.${serverMM.mmid}/ws",
          method = PureMethod.GET,
          channel = channelDeferred,
        )
      )
      val channel = channelDeferred.await()
      val ctx = channel.start()
      var res = "hi~0"
      for (i in ctx.income) {
        res = i.text
        println("QWQ client $i")
      }
      println("TEST DONE")
      assertEquals("hi~${serverMM.MAX}", res)
    }

    dnsRuntime.shutdown()
  }

  @Test
  fun testWebSocket() = runCommonTest(100, timeout = 600.seconds, block = webSocketTester)

  @Test
  fun testFileFetch() = runCommonTest {
    val dns = DnsNMM()
    val httpMM = HttpNMM()
    dns.install(httpMM)
    val dnsRuntime = dns.bootstrap()
    val httpRuntime = dnsRuntime.open(httpMM.mmid)
    lateinit var data: ByteArray
    for (i in 1..1000) {
      val res = httpRuntime.nativeFetch(
        "file://http.std.dweb/fetch?url=http%3A%2F%2Flocalhost%3A12207%2Fm3-favicon.svg"
      ).binary()
      if (i == 1) {
        data = res
      } else {
        assertContentEquals(res, data, "error in $i")
      }
    }
  }

  @Test
  fun testHttpFetch() = runCommonTest {
    val dns = DnsNMM()
    val httpMM = HttpNMM()
    dns.install(httpMM)
    val dnsRuntime = dns.bootstrap()
    val httpRuntime = dnsRuntime.open(httpMM.mmid)
    httpRuntime.nativeFetch("file://http.std.dweb/ready")

    lateinit var data: ByteArray
    for (i in 1..1000) {
      val res = httpRuntime.nativeFetch(
        "https://http.std.dweb/fetch?url=http%3A%2F%2Flocalhost%3A12207%2Fm3-favicon.svg"
      ).binary()
      if (i == 1) {
        data = res
      } else {
        assertContentEquals(res, data, "error in $i")
      }
    }
  }
}