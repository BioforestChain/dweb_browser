import org.dweb_browser.browser.jsProcess.JsProcessNMM
import org.dweb_browser.browser.jsProcess.ext.createJsProcess
import org.dweb_browser.core.http.router.bindPrefix
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.core.std.http.HttpNMM
import org.dweb_browser.helper.addDebugTags
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test

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

class TestNMM(mmid: String = "test.ipcPool.dweb", name: String) :
  NativeMicroModule(mmid, name) {
  inner class TestRuntime(override val bootstrapContext: BootstrapContext) :
    NativeRuntime() {
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

  @Test
  fun testCreateMessagePortIpc() = runCommonTest {
    val dns = DnsNMM()
    val jsProcessNMM = JsProcessNMM()
    dns.install(jsProcessNMM)
    val httpNMM = HttpNMM()
    dns.install(httpNMM)
    val fileNMM = FileNMM()
    dns.install(fileNMM)
    val testNMM = TestNMM("xx.dweb", "xx")
    dns.install(testNMM)
    val dnsRunTime = dns.bootstrap()
    val testRuntime = dnsRunTime.open(testNMM.mmid) as TestNMM.TestRuntime

    val jsProcess = testRuntime.createJsProcess("hhh")
    jsProcess.defineRoutes {
      "/" bindPrefix PureMethod.GET by defineStringResponse {
        println("QAQ request.url=${request.url}")
        request.url.toString()
        when (request.url.encodedPath) {
          "/index.js" -> "import {a} from './a';console.log('a=',a)"
          "/a" -> "export const a=1"
          else -> "console.error('should not load:',import.meta.url)"
        }
      }
    }
    jsProcess.codeIpc.awaitClosed()
  }
}