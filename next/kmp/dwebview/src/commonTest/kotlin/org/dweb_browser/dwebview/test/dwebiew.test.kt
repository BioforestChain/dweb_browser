package org.dweb_browser.dwebview.test

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class DWebViewTest {
  companion object {

    suspend inline fun getWebview(): IDWebView {
      debugDWebView.forceEnable()
      val mm = object : NativeMicroModule("mm.test.dweb", "MM") {
        override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {}
        override suspend fun _shutdown() {}
      }
      return IDWebView.create(mm)
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
    println("icon.image=${dwebview.getFavoriteIcon()}")
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
    dwebview.onReady.awaitOnce()
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
    val ready = CompletableDeferred<Boolean>()
    dwebview.onReady {
      ready.complete(true)
    }

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

  @Test
  fun setContentScale() = runCommonTest {
    val dwebview = getWebview()
    val defaultWidth = dwebview.evaluateAsyncJavascriptCode("window.innerWidth").toFloat()
    val defaultHeight = dwebview.evaluateAsyncJavascriptCode("window.innerHeight").toFloat()
    val defaultDensity = dwebview.evaluateAsyncJavascriptCode("window.devicePixelRatio").toFloat()
    val scale = 0.5f;
    dwebview.setContentScale(scale, defaultWidth, defaultHeight, defaultDensity)
    val scaleWidth = dwebview.evaluateAsyncJavascriptCode("window.innerWidth").toFloat()
    val scaleHeight = dwebview.evaluateAsyncJavascriptCode("window.innerHeight").toFloat()
    val scaleDensity = dwebview.evaluateAsyncJavascriptCode("window.devicePixelRatio").toFloat()

    assertEquals(defaultWidth, scaleWidth * scale, "defaultWidth = scaleWidth * scale")
    assertEquals(defaultHeight, scaleHeight * scale, "defaultHeight = scaleHeight * scale")
    /// 在一些平台上，缩放是整个webview图层上的，因此该参数可能无法通过
    assertEquals(defaultDensity * scale, scaleDensity, "defaultDensity * scale = scaleDensity")
  }
}
