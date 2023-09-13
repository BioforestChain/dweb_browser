package info.bagen.dwebbrowser

import org.dweb_browser.helper.keepFileParameters
import org.junit.Test

class UrlHelperTest {
  @Test
  fun `file url parameters test`() {
    val urlString = "file://http.std.dweb/listen?token=1234&domain=abc"

    println(urlString.keepFileParameters())
    println(urlString.keepFileParameters().toString())
    println(urlString.keepFileParameters().parameters)
  }
}