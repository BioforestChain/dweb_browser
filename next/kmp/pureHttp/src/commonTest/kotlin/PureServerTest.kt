import kotlinx.coroutines.launch
import org.dweb_browser.pure.http.HttpPureServer
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureTextFrame
import org.dweb_browser.pure.http.defaultHttpPureClient
import org.dweb_browser.pure.http.fetch
import org.dweb_browser.pure.http.websocket
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PureServerTest {
  @Test
  fun testHttp() = runCommonTest {
    val server = HttpPureServer { req ->
      PureResponse.build {
        body(req.url.encodedPath)
      }
    }
    val port = server.start(0u)

    assertEquals("/hi", defaultHttpPureClient.fetch(url = "http://127.0.0.1:${port}/hi").text())

    server.close()
  }

  @Test
  fun testWebSocket() = runCommonTest {
    val server = HttpPureServer { req ->
      if (req.url.encodedPath == "/echo") {
        req.byChannel {
          start().apply {
            launch {
              for (msg in income) {
                outgoing.send(msg)
              }
            }
          }
        }
      } else null
    }
    val port = server.start(0u)

    val channel = defaultHttpPureClient.websocket(url = "ws://127.0.0.1:${port}/echo")
    channel.start().apply {
      val incomeRes = mutableListOf<String>()
      val outgoingRes = mutableListOf<String>()

      launch {
        for (echoMsg in income) {
          require(echoMsg is PureTextFrame)
          incomeRes.add(echoMsg.text)
        }
        assertEquals(incomeRes.joinToString("\n"), outgoingRes.joinToString("\n"))
      }
      for (i in 1..10) {
        val outMsg = "msg$i"
        outgoing.send(PureTextFrame(outMsg))
        outgoingRes.add(outMsg)
      }
      close(null)
    }

    server.close()
  }
}