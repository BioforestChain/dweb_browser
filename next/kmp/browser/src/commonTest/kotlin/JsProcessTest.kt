import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import org.dweb_browser.browser.jsProcess.JsProcessNMM
import org.dweb_browser.browser.jsProcess.ext.createJsProcess
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.bindPrefix
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.core.std.http.HttpNMM
import org.dweb_browser.helper.addDebugTags
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

//class TestHttpMicroModule(mmid: String = "http.server.dweb") :
//  NativeRuntime(mmid, "test IpcPool") {
//
//  init {
//    GlobalScope.launch {
//      /// 初始化DNS服务
//      val dnsNMM = DnsNMM()
//      BootNMM().also {
//        dnsNMM.install(it)
//      }
//      HttpNMM().also {
//        dnsNMM.install(it)
//      }
//      dnsNMM.bootstrap()
//    }
//  }
//
//  private val JS_PROCESS_WORKER_CODE by lazy {
//    mmScope.async {
//      nativeFetch("file:///sys/browser/js-process.worker/index.js").binary()
//    }
//  }
//  private val INTERNAL_PATH = "/<internal>".encodeURI()
//  private val JS_CORS_HEADERS = mapOf(
//    Pair("Content-Type", "text/javascript"),
//    Pair("Access-Control-Allow-Origin", "*"),
//    Pair("Access-Control-Allow-Headers", "*"),// 要支持 X-Dweb-Host
//    Pair("Access-Control-Allow-Methods", "*"),
//  )
//
//  suspend fun createServer(): HttpDwebServer {
//    return this.createHttpDwebServer(DwebHttpServerOptions()).also { server ->
//      // 提供基本的主页服务
//      val serverIpc = server.listen();
//      serverIpc.onRequest.onEach { request ->
//        // <internal>开头的是特殊路径，给Worker用的，不会拿去请求文件
//        if (request.uri.encodedPath.startsWith(INTERNAL_PATH)) {
//          val internalPath = request.uri.encodedPath.substring(INTERNAL_PATH.length)
//          if (internalPath == "/bootstrap.js") {
//            serverIpc.postMessage(
//              IpcResponse.fromBinary(
//                request.reqId,
//                200,
//                PureHeaders(JS_CORS_HEADERS),
//                JS_PROCESS_WORKER_CODE.await(),
//                serverIpc
//              )
//            )
//          } else {
//            serverIpc.postMessage(
//              IpcResponse.fromText(
//                request.reqId,
//                404,
//                PureHeaders(JS_CORS_HEADERS),
//                "// no found $internalPath",
//                serverIpc
//              )
//            )
//          }
//        } else {
//          val response = nativeFetch("file:///sys/browser/js-process.main${request.uri.fullPath}")
//          serverIpc.postMessage(
//            IpcResponse.fromResponse(request.reqId, response, serverIpc)
//          )
//        }
//      }.launchIn(mmScope)
//    }
//  }
//
//  suspend fun createAPIS(): JsProcessWebApi {
//    /// 主页的网页服务
//    val mainServer = createServer()
//    val apis = createJsProcessWeb(mainServer, this)
//    val bootstrapUrl =
//      apis.dWebView.resolveUrl(mainServer.startResult.urlInfo.buildInternalUrl { resolvePath("$INTERNAL_PATH/bootstrap.js") }
//        .toString())
//    return apis
//  }
//
//  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
//  }
//
//  override suspend fun _shutdown() {
//    TODO("Not yet implemented")
//  }
//
//}

class TestNMM(mmid: String = "test.ipcPool.dweb", name: String) : NativeMicroModule(mmid, name) {
  inner class TestRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    override suspend fun _bootstrap() {

    }

    override suspend fun _shutdown() {

    }

  }

  override fun createRuntime(bootstrapContext: BootstrapContext): Runtime {
    return TestRuntime(bootstrapContext)
  }

}

class JsProcessTest {


  init {
    addDebugTags(listOf("/.+/"))
  }

  inner class TestContext {
    val dns = DnsNMM()
    val jsProcessNMM = JsProcessNMM()
    val httpNMM = HttpNMM()
    val fileNMM = FileNMM()
    val testNMM = TestNMM("xx.dweb", "xx")
    lateinit var dnsRunTime: DnsNMM.DnsRuntime
      private set
    lateinit var testRuntime: TestNMM.TestRuntime
      private set

    suspend fun init() {
      dns.install(jsProcessNMM)
      dns.install(httpNMM)
      dns.install(fileNMM)
      dns.install(testNMM)
      dnsRunTime = dns.bootstrap()
      testRuntime = dnsRunTime.open(testNMM.mmid) as TestNMM.TestRuntime
    }
  }

  suspend fun buildJsProcessTestContext(cb: suspend TestContext.() -> Unit) {
    TestContext().run {
      init()
      cb()
      dnsRunTime.shutdown()
    }
  }

  @Test
  fun testCreateProcess() = runCommonTest(timeout = 600.seconds) {
    buildJsProcessTestContext {
      val actual = randomUUID()
      println("QAQ actual=$actual")
      val jsProcess = testRuntime.createJsProcess("hhh")
      jsProcess.defineEsm {
        "/" bindPrefix PureMethod.GET by defineStringResponse {
          println("QAQ request.url=${request.url}")
          request.url.toString()
          when (request.url.encodedPath) {
            "/index.js" -> """
            import {a} from './a';
            const ipcEvent = navigator.dweb.ipc.IpcEvent.fromText('test',a)
            navigator.dweb.jsProcess.fetchIpc.postMessage(ipcEvent)
          """.trimIndent()

            "/a" -> "export const a=`$actual`"
            else -> "console.error('should not load:',import.meta.url)"
          }
        }
      }
      val expected = jsProcess.fetchIpc.onEvent("wait-js").mapNotNull { event ->
        event.consumeFilter { it.name == "test" }?.text
      }.first()
      println("QAQ expected=$expected")
      assertEquals(actual, expected)
    }
  }

  @Test
  fun testHttpInJsProcess() = runCommonTest(timeout = 600.seconds) {
    buildJsProcessTestContext {
      val jsProcess = testRuntime.createJsProcess("js-http-demo")
      jsProcess.defineEsm {
        "/index.js" bind PureMethod.GET by defineStringResponse {
          """
            const { http, jsProcess, ipc } = navigator.dweb;
            const httpServer = await http.createHttpDwebServer(jsProcess, { subdomain: "www" });
            jsProcess.fetchIpc.postMessage(
              ipc.IpcEvent.fromText("http-server", httpServer.startResult.urlInfo.buildDwebUrl().href)
            );
            await httpServer.listen((event) => {
              console.log("got request", event.ipcRequest.url);
              return { body: event.ipcRequest.url };
            });
          """.trimIndent()
        }
      }

      val jsHttpUrl = jsProcess.fetchIpc.onEvent("wait-js").mapNotNull { event ->
        event.consumeFilter { it.name == "http-server" }?.text
      }.first()
      println("QAQ jsHttpUrl=$jsHttpUrl")

      val actual = "/${randomUUID()}"
      val expected = testRuntime.nativeFetch("$jsHttpUrl$actual").text()
      println("QAQ expected=$expected")
      assertEquals(actual, expected)
    }
  }
}