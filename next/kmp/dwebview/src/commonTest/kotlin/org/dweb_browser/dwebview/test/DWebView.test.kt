package org.dweb_browser.dwebview.test

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import org.dweb_browser.core.ipc.helper.DWebMessage
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.core.std.http.HttpNMM
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.test.runCommonTest
import kotlin.properties.Delegates
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
class DWebViewTest {
  companion object {

    class TestNMM : NativeMicroModule("mm.test.dweb", "MM") {
      inner class TestRuntime(override val bootstrapContext: BootstrapContext) :
        NativeMicroModule.NativeRuntime() {
        override suspend fun _bootstrap() {}
        override suspend fun _shutdown() {}
      }

      override fun createRuntime(bootstrapContext: BootstrapContext): Runtime {
        return TestRuntime(bootstrapContext)
      }
    }

    class PrepareContext(
    ) {
      val dnsNMM = DnsNMM()
      val testNMM = TestNMM()
      lateinit var dnsRuntime: DnsNMM.DnsRuntime
        private set
      lateinit var testRuntime: MicroModule.Runtime
        private set

      suspend fun init() {
        dnsNMM.install(testNMM)
        dnsRuntime = dnsNMM.bootstrap()
        testRuntime = dnsRuntime.open(testNMM.mmid)
      }
    }

    val getPrepareContext = SuspendOnce {
      PrepareContext().apply { init() }
    }

    suspend inline fun getWebview(options: DWebViewOptions = DWebViewOptions()): IDWebView {
      debugDWebView.forceEnable()
      val ctx = getPrepareContext()
      return IDWebView.create(ctx.testRuntime, options)
    }

  }

  @Test
  fun evalJavascript() = runCommonTest {
    val dwebview = getWebview()
    assertEquals("2", dwebview.evaluateAsyncJavascriptCode("1+1"))
    assertEquals("3", dwebview.evaluateAsyncJavascriptCode("await Promise.resolve(1)+2"))
    assertEquals("catch", runCatching {
      dwebview.evaluateAsyncJavascriptCode("await Promise.reject('no-catch')")
    }.getOrElse {
      assertEquals("no-catch", it.message)
      "catch"
    })

    dwebview.destroy()
  }

  @Test
  fun getUA() = runCommonTest {
    val dwebview = getWebview()
    dwebview.loadUrl("https://www.baidu.com")
    println(dwebview.evaluateAsyncJavascriptCode("JSON.stringify(navigator.userAgentData.brands)"))
    assertEquals(
      "true",
      dwebview.evaluateAsyncJavascriptCode("navigator.userAgentData.brands.filter((item)=>item.brand=='DwebBrowser').length>0")
    )
  }

  @Test
  fun getIcon() = runCommonTest {

    val dwebview = getWebview()
    dwebview.loadUrl("https://www.baidu.com")
    assertEquals("https://www.baidu.com/favicon.ico", dwebview.getIcon())
    println("icon.image=${dwebview.getIconBitmap()}")
  }

  @Test
  fun onScroll() = runCommonTest {
    val dwebview = getWebview()
    dwebview.loadUrl("https://www.baidu.com")

    dwebview.evaluateAsyncJavascriptCode("document.body.style.height='10000px'")
    val isScrolled = CompletableDeferred<Int>()
    dwebview.onScroll {
      debugDWebView("scrollY", it.scrollY)
      isScrolled.complete(it.scrollY)
    }

    dwebview.evaluateAsyncJavascriptCode("scrollTo(0,100),console.log('scrollTo 100')")
    dwebview.evaluateAsyncJavascriptCode("scrollTo(0,200),console.log('scrollTo 200')")

    assertNotEquals(0, select {
      isScrolled.onAwait { it }
      onTimeout(1000) { 0 }
    })
  }

  @Test
  fun onCreateWindow() = runCommonTest {
    val dwebview = getWebview()
    dwebview.loadUrl("https://www.baidu.com")
    val isCreated = CompletableDeferred<IDWebView>()
    dwebview.onCreateWindow {
      debugDWebView("onCreateWindow", it)
      isCreated.complete(it)
    }
    val openUrl = "https://image.baidu.com/"
    dwebview.evaluateAsyncJavascriptCode("void open('$openUrl')")
    assertEquals(openUrl, select {
      isCreated.onAwait { it.getUrl() }
      onTimeout(2000) { "<no-browser-open>" }
    })
  }

  @Test
  fun closeWatcher() = runCommonTest {
    val dwebview = getWebview()
    /// 以下是一般情况
    dwebview.loadUrl("https://example.com/")
    assertEquals(true, dwebview.canGoBack())
    println("TEST doGoBack")
    dwebview.goBack()
    assertEquals(false, dwebview.canGoBack())
    delay(100)
    assertEquals("about:blank", dwebview.getOriginalUrl())

    /// 以下是CloseWatcher改变goBack的情况
    dwebview.loadUrl("https://example.com/")
    assertEquals(true, dwebview.canGoBack())
    println("TEST create CloseWatcher")
    dwebview.evaluateAsyncJavascriptCode("new CloseWatcher()")
    delay(200)
    println("TEST doGoBack")
    dwebview.goBack()
    assertEquals(true, dwebview.canGoBack())
    delay(100)
    assertEquals("https://example.com/", dwebview.getOriginalUrl())
  }

  @Test
  fun onReady() = runCommonTest {
    val dwebview = getWebview()
    /// 以下是一般情况
    launch {
      dwebview.loadUrl("https://example.com")
    }
    dwebview.onReady.first()
    assertEquals("https://example.com/", dwebview.getOriginalUrl())
  }


  @Test
  fun onBeforeUnload() = runCommonTest {
    val dwebview = getWebview()
    dwebview.loadUrl("https://example.com")
    delay(1000)
    dwebview.evaluateAsyncJavascriptCode(
      "void addEventListener('beforeunload', event=>{event.preventDefault();  event.returnValue = true;})"
    )
    val isEmitted = CompletableDeferred<Boolean>()
    dwebview.onBeforeUnload {
      it.hook("cancel close").keepDocument()
      isEmitted.complete(true)
    }
    launch {
      debugDWebView("TEST doClose")
      delay(200)
      dwebview.requestClose()
    }

    delay(1000)

    assertTrue {
      select {
        isEmitted.onAwait { it }
        onTimeout(1000) { false }
      }
    }
  }

  @Test
  fun loadingProgressFlow() = runCommonTest {
    val dwebview = getWebview()
    val progressList = mutableListOf<Float>()
    launch {
      dwebview.loadingProgressFlow.collect {
        progressList.add(it)
      }
    }
    dwebview.loadUrl("https://example.com")
    dwebview.onReady.first()

    println("TEST progressList=${progressList.joinToString { "$it" }}")
    assertTrue(progressList.isNotEmpty())
  }

  @Test
  fun onDestroy() = runCommonTest {
    val dwebview = getWebview()
    val isDestroyed = CompletableDeferred<Boolean>()
    dwebview.onDestroy {
      isDestroyed.complete(true)
    }
    dwebview.requestClose()
    assertTrue {
      select {
        isDestroyed.onAwait { it }
        onTimeout(1000) { false }
      }
    }
  }

  @Test
  fun setScrollBarVisible() = runCommonTest {
    val dwebview = getWebview()
    assertEquals(
      "true",
      dwebview.evaluateAsyncJavascriptCode("document.body.clientWidth==window.innerWidth"),
      "default body-width equals window-width"
    )
    dwebview.evaluateAsyncJavascriptCode("document.body.style.height='10000px'")
    assertEquals(
      "false",
      dwebview.evaluateAsyncJavascriptCode("document.body.clientWidth==window.innerWidth"),
      "body-width with scrollbar no equals window-width"
    )
    dwebview.setHorizontalScrollBarVisible(false)
    dwebview.setVerticalScrollBarVisible(false)
    assertEquals(
      "true",
      dwebview.evaluateAsyncJavascriptCode("document.body.clientWidth==window.innerWidth"),
      "body-width with hidden-scrollbar equals window-width"
    )
  }

//  @Test
//  fun setContentScale() = runCommonTest {
//    val dwebview = getWebview()
//    val defaultWidth = dwebview.evaluateAsyncJavascriptCode("window.innerWidth").toFloat()
//    val defaultHeight = dwebview.evaluateAsyncJavascriptCode("window.innerHeight").toFloat()
//    val defaultDensity = dwebview.evaluateAsyncJavascriptCode("window.devicePixelRatio").toFloat()
//    val scale = 0.5f;
//    dwebview.setContentScale(scale, defaultWidth, defaultHeight, defaultDensity)
//    val scaleWidth = dwebview.evaluateAsyncJavascriptCode("window.innerWidth").toFloat()
//    val scaleHeight = dwebview.evaluateAsyncJavascriptCode("window.innerHeight").toFloat()
//    val scaleDensity = dwebview.evaluateAsyncJavascriptCode("window.devicePixelRatio").toFloat()
//
//    assertEquals(defaultWidth, scaleWidth * scale, "defaultWidth = scaleWidth * scale")
//    assertEquals(defaultHeight, scaleHeight * scale, "defaultHeight = scaleHeight * scale")
//    /// 在一些平台上，缩放是整个webview图层上的，因此该参数可能无法通过
//    assertEquals(defaultDensity * scale, scaleDensity, "defaultDensity * scale = scaleDensity")
//  }

  @Test
  fun testHttpProxy() = runCommonTest(timeout = 600.seconds) {
    val ctx = getPrepareContext()
    val fileNMM = FileNMM()
    ctx.dnsNMM.install(fileNMM)
    val httpNMM = HttpNMM()
    ctx.dnsNMM.install(httpNMM)
    ctx.dnsRuntime.open(httpNMM.mmid)
    val dwebview = getWebview(DWebViewOptions(openDevTools = true))
    ctx.testRuntime.nativeFetch("file://http.std.dweb/ready")

    var byteLength by Delegates.notNull<Int>()
    for (i in 1..1000) {
      val contentLength = runCatching {
        dwebview.evaluateAsyncJavascriptCode("fetch('https://http.std.dweb/fetch?url=http%3A%2F%2Flocalhost%3A12207%2Fm3-favicon.svg').then(res=>res.arrayBuffer()).then(buf=>buf.byteLength)")
          .toInt()
      }.getOrElse {
        println(it)
        0
      }
      println("test in $i contentLength=$contentLength")
      if (i == 1) {
        byteLength = contentLength
      } else {
        if (contentLength != byteLength) {
          println("error in $i")
          delay(100000)
        }
        assertEquals(contentLength, byteLength, "error in $i")
      }
    }
  }

  @Test
  fun testMessagePort() = runCommonTest(1, timeout = 600.seconds) { time ->
    println("test-$time")
    val ctx = getPrepareContext()
    val fileNMM = FileNMM()
    ctx.dnsNMM.install(fileNMM)
    val dwebview = getWebview(DWebViewOptions(openDevTools = true))
    dwebview.loadUrl("http://localhost:12207")
    val messageChannel = dwebview.createMessageChannel()
    /// prepare
    dwebview.evaluateAsyncJavascriptCode(
      """
      window.addEventListener("message", (event) => {
        if (event.data === "okk") {
          const port = event.ports[0];
          port.addEventListener("message", (event) => {
            console.log(event.data)
            port.postMessage(event.data);
          });
          port.start()
        }
      })
    """.trimIndent()
    )

    dwebview.postMessage("okk", listOf(messageChannel.port1))

    /// send
    val actual = mutableListOf<String>()
    launch {
      messageChannel.port2.start()
      for (i in 1..10000) {
        val msg = "$i:${randomUUID()}"
        actual += msg
        messageChannel.port2.postMessage(DWebMessage.DWebMessageString(msg))
      }
      // 注意，这里的close是直接关闭js侧的读和写，所以可能会导致数据丢失
      messageChannel.port2.close()
      println("close")
    }

    // receive
    val expected = mutableListOf<String>()
    for (event in messageChannel.port2.onMessage) {
      expected += event.text
    }

    runCatching {
      assertContentEquals(expected, actual)
    }.getOrElse {
      println(it)
      delay(10000000)
      throw it
    }
  }
}
