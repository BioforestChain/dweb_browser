package org.dweb_browser.pure.http

import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import org.dweb_browser.pure.http.ktor.KtorPureClient

actual class HttpPureClient : KtorPureClient<OkHttpConfig>(OkHttp, {
  engine {
    config {
      sslSocketFactory(SslSettings.getSslContext().socketFactory, SslSettings.trustManager)
    }
  }
}) {

}