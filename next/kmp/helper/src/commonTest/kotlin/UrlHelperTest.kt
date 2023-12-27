package info.bagen.dwebbrowser

import org.dweb_browser.helper.toIpcUrl
import kotlin.test.Test

class UrlHelperTest {
  @Test
  fun fileUrlParametersTest() {
    val urlString = "file://http.std.dweb/listen?token=1234&domain=abc"

    println(urlString.toIpcUrl())
    println(urlString.toIpcUrl().toString())
    println(urlString.toIpcUrl().parameters)
  }
}