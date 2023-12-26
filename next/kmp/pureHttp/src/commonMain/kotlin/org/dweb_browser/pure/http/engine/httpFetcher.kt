package org.dweb_browser.pure.http.engine

import io.ktor.client.HttpClient
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.websocket.WebSockets

private var fetcher = HttpClient(getKtorClientEngine()) {
  install(ContentEncoding)
  install(WebSockets)
}
val httpFetcher get() = fetcher
fun setHttpFetcher(client: HttpClient) {
  fetcher = client
}
