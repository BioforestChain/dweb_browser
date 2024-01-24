
import io.ktor.util.sha1
import org.dweb_browser.pure.http.defaultHttpPureClient
import org.dweb_browser.pure.http.fetch
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PureClientTest {
  @OptIn(ExperimentalStdlibApi::class)
  @Test
  fun testHttp2Request() = runCommonTest {
    val href = "https://source.dwebdapp.com/dweb-browser-apps/dweb-app-assets/btcmeta/desc_02.jpg"
    val res = defaultHttpPureClient.fetch(url = href)
    val contentLength = res.headers.get("Content-Length")
    val contentBody = res.body.toPureBinary()
    val contentSha1 = sha1(contentBody).toHexString()
    println("headers=${res.headers.toList().joinToString("\n\t") { "${it.first}=${it.second}" }}")
    println("contentLength=$contentLength contentSha1=$contentSha1")
    assertEquals(contentLength, "191933")
    assertEquals(contentSha1, "e21ca651e2936c9f6be7b2fce825aa527d4c079c")
  }
}