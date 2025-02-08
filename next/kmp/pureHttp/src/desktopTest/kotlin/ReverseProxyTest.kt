import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Url
import org.dweb_browser.pure.http.HttpPureServer
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.ReverseProxyServer
import org.dweb_browser.pure.http.SslSettings
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ReverseProxyTest {
  @Test
  fun start() = runCommonTest {
    val dwebServer = HttpPureServer { req ->
      PureResponse.build { body(req.url.encodedPathAndQuery) }
    }
    val dwebServerPort = dwebServer.start(0u)
    println("DwebServerPort=${dwebServerPort}")

    val reverseProxyServer = ReverseProxyServer()
    val reverseProxyPort = reverseProxyServer.start(dwebServerPort)
    println("ReverseProxy=${reverseProxyPort}")

    val client = HttpClient(OkHttp) {
      engine {
        proxy = ProxyBuilder.http(Url("http://127.0.0.1:$reverseProxyPort/"))
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

    val responseText = client.request("https://baidu.com.dweb/a/b?c=1").bodyAsText()
    println("responseText=$responseText")
    assertEquals("/a/b?c=1", responseText)
    dwebServer.close()
    reverseProxyServer.close()
  }
}