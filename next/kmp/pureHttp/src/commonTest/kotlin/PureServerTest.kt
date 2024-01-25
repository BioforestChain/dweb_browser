import org.dweb_browser.pure.http.HttpPureClient
import org.dweb_browser.pure.http.HttpPureServer
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.fetch
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PureServerTest {
  @Test
  fun startServer() = runCommonTest {
    val server = HttpPureServer { req ->
      PureResponse.build {
        body(req.url.encodedPath)
      }
    }
    val port = server.start(0u)

    val client = HttpPureClient()
    assertEquals("/hi", client.fetch(url = "http://127.0.0.1:${port}/hi").text())

    server.close()
  }
}