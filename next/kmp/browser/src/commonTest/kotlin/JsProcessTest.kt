import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.dweb_browser.browser.jmm.JsMicroModule
import org.dweb_browser.browser.jsProcess.JsProcessNMM
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.http.router.HttpHandlerToolkit
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.bindPrefix
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.core.std.http.HttpNMM
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.addDebugTags
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds


class TestJmm(mmid: String = "test.ipcPool.dweb", name: String) :
  JsMicroModule(JmmAppInstallManifest().also {
    it.id = mmid
    it.name = name
  }) {
  inner class TestJmmRuntime(bootstrapContext: BootstrapContext) :
    JsMicroModule.JmmRuntime(bootstrapContext) {
    override val esmLoader: HttpHandlerToolkit.() -> Unit
      get() = {}
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = TestJmmRuntime(bootstrapContext)
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
    val test1NMM = TestJmm("test1.dweb", "test1")
    val test2NMM = TestJmm("test1.dweb", "test2")

    //    val testJmmNMM = JsMicroModule(JmmAppInstallManifest().apply {
//      id = "test1.jmm.dweb"
//      name = "test1-jmm"
//    })
    lateinit var dnsRunTime: DnsNMM.DnsRuntime
      private set

    suspend fun init() {
      dns.install(jsProcessNMM)
      dns.install(httpNMM)
      dns.install(fileNMM)
      dns.install(test1NMM)
      dns.install(test2NMM)
      dnsRunTime = dns.bootstrap()
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
      val testRuntime = dnsRunTime.open(test1NMM.mmid) as TestJmm.TestJmmRuntime
      val jsProcess = testRuntime.getJsProcess()
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
      val testRuntime = dnsRunTime.open(test1NMM.mmid) as TestJmm.TestJmmRuntime
      val jsProcess = testRuntime.getJsProcess()
      jsProcess.defineEsm {
        "/index.js" bind PureMethod.GET by defineStringResponse {
          """
            const { http, jsProcess, ipc } = navigator.dweb;
            const httpServer = await http.createHttpDwebServer(jsProcess, { subdomain: "www" });
            await httpServer.listen((event) => {
              console.log("got request", event.ipcRequest.url);
              return { body: event.ipcRequest.parsed_url.pathname.slice(1) };
            });
            jsProcess.fetchIpc.postMessage(
              ipc.IpcEvent.fromText("http-server", httpServer.startResult.urlInfo.buildDwebUrl().href)
            );
          """.trimIndent()
        }
      }

      val jsHttpUrl = jsProcess.fetchIpc.onEvent("wait-js").mapNotNull { event ->
        event.consumeFilter { it.name == "http-server" }?.text
      }.first()
      println("QAQ jsHttpUrl=$jsHttpUrl")

      val actual = randomUUID()
      val expected = testRuntime.nativeFetch("$jsHttpUrl$actual").text()
      println("QAQ expected=$expected")
      assertEquals(actual, expected)
    }
  }


  @Test
  fun testJmmConnectJmm() = runCommonTest(timeout = 600.seconds) {
    buildJsProcessTestContext {
      val actual = randomUUID()
      println("QAQ actual=$actual")
      val test1Runtime = dnsRunTime.open(test1NMM.mmid) as TestJmm.TestJmmRuntime
      val jsProcess1 = test1Runtime.getJsProcess()
      jsProcess1.defineEsm {
        "/index.js" bind PureMethod.GET by defineStringResponse {
          """
            const { jsProcess, ipc } = navigator.dweb;
            const ipc2 = await jsProcess.connect(`${test2NMM.mmid}`)
            ipc2.postMessage( ipc.IpcEvent.fromText("js2js", `$actual`) );
          """.trimIndent()
        }
      }
      val test2Runtime = dnsRunTime.open(test2NMM.mmid) as TestJmm.TestJmmRuntime
      val jsProcess2 = test2Runtime.getJsProcess()
      jsProcess2.defineEsm {
        "/index.js" bind PureMethod.GET by defineStringResponse {
          """
            const { jsProcess } = navigator.dweb;
            
            const ipc1 = await jsProcess.connect(`${test1NMM.mmid}`);
            const ipc3 = await jsProcess.connect(`${fileNMM.mmid}`);
            ipc1.onEvent("js2js").collect((event) => ipc3.postMessage(event.consume()));
          """.trimIndent()
        }
      }

      val test3Runtime = dnsRunTime.open(fileNMM.mmid) as FileNMM.FileRuntime
      val ipc3 = test3Runtime.connect(test2Runtime.mmid)
      val result = ipc3.onEvent("js2js").map { it.consume() }.first()
      val expected = result.text
      println("QAQ expected=$expected")
      assertEquals(actual, expected)
    }
  }

  @Test
  fun testHttpInJsProcessForBrowser() = runCommonTest(timeout = 600.seconds) {
    buildJsProcessTestContext {
      val testRuntime = dnsRunTime.open(test1NMM.mmid) as TestJmm.TestJmmRuntime
      val jsProcess = testRuntime.getJsProcess()
      jsProcess.defineEsm {
        "/index.js" bind PureMethod.GET by defineStringResponse {
          """
            const { http, jsProcess, ipc } = navigator.dweb;
            const httpServer = await http.createHttpDwebServer(jsProcess, { subdomain: "www" });
            await httpServer.listen(async (event) => {
              console.log("got request", event.ipcRequest.url);
              const response = await jsProcess.nativeFetch(`http://localhost:12207`+event.ipcRequest.parsed_url.pathname);
              // return response;
              return {
                status: response.status,
                headers: response.headers,
                body: response.body
              }
            });
            jsProcess.fetchIpc.postMessage(
              ipc.IpcEvent.fromText("http-server", httpServer.startResult.urlInfo.buildDwebUrl((url) => {
                url.pathname="p2p.svg"
              }).href)
            );
          """.trimIndent()
        }
      }

      val jsHttpUrl = jsProcess.fetchIpc.onEvent("wait-js").mapNotNull { event ->
        event.consumeFilter { it.name == "http-server" }?.text
      }.first()
      println("QAQ jsHttpUrl=$jsHttpUrl")

      val dwebview = IDWebView.create(testRuntime, DWebViewOptions(openDevTools = true))
      dwebview.loadUrl(jsHttpUrl)

      delay(10000000)
    }

  }
}