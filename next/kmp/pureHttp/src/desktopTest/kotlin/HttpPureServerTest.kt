import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import org.dweb_browser.pure.http.HttpPureServer
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.SslSettings
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpPureServerTest {
  @Test
  fun start() = runCommonTest {
    val dwebServer = HttpPureServer { req ->
      PureResponse.build { body(req.url.encodedPathAndQuery) }
    }
    val dwebServerPort = dwebServer.start(0u, true)
    println("DwebServerPort=${dwebServerPort}")
    val client = HttpClient(OkHttp) {
      engine {
        config {
          sslSocketFactory(
            SslSettings.getSslContext().socketFactory,
            SslSettings.trustManager
          )
          hostnameVerifier { hostname, _ ->
            hostname.endsWith(".dweb") || hostname == "127.0.0.1"
          }
        }
      }
    }

    val responseText = client.request("https://127.0.0.1:$dwebServerPort/a/b?c=1").bodyAsText()
    println("responseText=$responseText")
    assertEquals("/a/b?c=1", responseText)
    dwebServer.close()
  }
}