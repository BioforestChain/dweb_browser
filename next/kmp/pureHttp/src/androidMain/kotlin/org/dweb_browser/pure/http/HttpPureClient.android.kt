package org.dweb_browser.pure.http

import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import org.dweb_browser.pure.http.ktor.KtorPureClient
import org.dweb_browser.pure.http.ktor.toKtorClientConfig

actual class HttpPureClient actual constructor(config: HttpPureClientConfig) :
  KtorPureClient<OkHttpConfig>(OkHttp, {
    config.toKtorClientConfig<OkHttpConfig>()()
    config.dwebSsl?.also { dwebSsl ->
      engine {
        preconfigured = OkHttpUtil.init(dwebSsl)
      }
    }
  }) {
}
