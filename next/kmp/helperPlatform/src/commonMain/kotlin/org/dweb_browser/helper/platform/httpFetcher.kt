package org.dweb_browser.helper.platform

import io.ktor.client.HttpClient
import io.ktor.client.plugins.compression.ContentEncoding

private var fetcher = HttpClient(getKtorClientEngine()) {
  install(ContentEncoding)
}
val httpFetcher get() = fetcher
fun setHttpFetcher(client: HttpClient) {
  fetcher = client
}
