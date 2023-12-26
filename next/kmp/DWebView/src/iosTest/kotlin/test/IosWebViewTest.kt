package test

import platform.WebKit.WKWebView
import kotlin.test.Test

class IosWebViewTest {
  @Test
  fun canGoBack() {
    val wv = WKWebView()
    println("canGoBack=${wv.canGoBack}")
  }
}