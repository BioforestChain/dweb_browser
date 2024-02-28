import io.ktor.http.fullPath
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.dweb_browser.browser.jsProcess.JsProcessWebApi
import org.dweb_browser.browser.jsProcess.createJsProcessWeb
import org.dweb_browser.core.ipc.IpcOptions
import org.dweb_browser.core.ipc.MessagePort
import org.dweb_browser.core.ipc.MessagePortIpc
import org.dweb_browser.core.ipc.helper.IpcResponse
import org.dweb_browser.core.ipc.kotlinIpcPool
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.http.DwebHttpServerOptions
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.core.std.http.HttpNMM
import org.dweb_browser.core.std.http.createHttpDwebServer
import org.dweb_browser.helper.encodeURI
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.sys.boot.BootNMM
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test

class TestHttpMicroModule(mmid: String = "http.server.dweb") :
  NativeMicroModule(mmid, "test IpcPool") {

  init {
    GlobalScope.launch {
      /// 初始化DNS服务
      val dnsNMM = DnsNMM()
      BootNMM().also {
        dnsNMM.install(it)
      }
      HttpNMM().also {
        dnsNMM.install(it)
      }
      dnsNMM.bootstrap()
    }
  }

  private val JS_PROCESS_WORKER_CODE by lazy {
    ioAsyncScope.async {
      nativeFetch("file:///sys/browser/js-process.worker/index.js").binary()
    }
  }
  private val INTERNAL_PATH = "/<internal>".encodeURI()
  private val JS_CORS_HEADERS = mapOf(
    Pair("Content-Type", "text/javascript"),
    Pair("Access-Control-Allow-Origin", "*"),
    Pair("Access-Control-Allow-Headers", "*"),// 要支持 X-Dweb-Host
    Pair("Access-Control-Allow-Methods", "*"),
  )

  suspend fun createServer(): HttpDwebServer {
    return this.createHttpDwebServer(DwebHttpServerOptions()).also { server ->
      // 提供基本的主页服务
      val serverIpc = server.listen();
      serverIpc.onRequest { (request, ipc) ->
        // <internal>开头的是特殊路径，给Worker用的，不会拿去请求文件
        if (request.uri.encodedPath.startsWith(INTERNAL_PATH)) {
          val internalPath = request.uri.encodedPath.substring(INTERNAL_PATH.length)
          if (internalPath == "/bootstrap.js") {
            ipc.postMessage(
              IpcResponse.fromBinary(
                request.reqId,
                200,
                PureHeaders(JS_CORS_HEADERS),
                JS_PROCESS_WORKER_CODE.await(),
                ipc
              )
            )
          } else {
            ipc.postMessage(
              IpcResponse.fromText(
                request.reqId, 404, PureHeaders(JS_CORS_HEADERS), "// no found $internalPath", ipc
              )
            )
          }
        } else {
          val response = nativeFetch("file:///sys/browser/js-process.main${request.uri.fullPath}")
          ipc.postMessage(
            IpcResponse.fromResponse(request.reqId, response, ipc)
          )
        }
      }
    }
  }

  suspend fun createAPIS(): JsProcessWebApi {
    /// 主页的网页服务
    val mainServer = createServer()
    val apis = createJsProcessWeb(mainServer, this)
    val bootstrapUrl =
      apis.dWebView.resolveUrl(mainServer.startResult.urlInfo.buildInternalUrl { resolvePath("$INTERNAL_PATH/bootstrap.js") }
        .toString())
    return apis
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }

}

class TestMicroModule(mmid: String = "test.ipcPool.dweb") :
  NativeMicroModule(mmid, "test IpcPool") {
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    TODO("Not yet implemented")
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }

}

class MessagePortTest {

  val mm = TestHttpMicroModule()

  @Test
  fun testCreateMessagePortIpc() = runCommonTest {
    val apis = mm.createAPIS()
    val clientMM = TestMicroModule("from.mm.dweb")
    val serverMM = TestMicroModule("to.mm.dweb")
    val channel = apis.dWebView.createMessageChannel()
    val port1 = channel.port1
    val port2 = channel.port2
    val ipcClient =
      kotlinIpcPool.create<MessagePortIpc>(
        "create-process-client",
        IpcOptions(clientMM, port = MessagePort.from(port1))
      )
    val ipcServer =
      kotlinIpcPool.create<MessagePortIpc>(
        "create-process-server",
        IpcOptions(serverMM, port = MessagePort.from(port2))
      )
    ipcServer.onLifeCycle {
      println("🧨 ipcServer=> ${it.event.state}")
    }
    ipcClient.onLifeCycle {
      println("🧨 ipcClient=> ${it.event.state}")
    }
  }
}