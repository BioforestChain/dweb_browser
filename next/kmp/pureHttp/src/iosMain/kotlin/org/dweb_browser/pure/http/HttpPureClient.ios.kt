package org.dweb_browser.pure.http

import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinClientEngineConfig
import org.dweb_browser.pure.http.ktor.KtorPureClient
import org.dweb_browser.pure.http.ktor.toKtorClientConfig

actual class HttpPureClient actual constructor(config: HttpPureClientConfig) :
  KtorPureClient<DarwinClientEngineConfig>(Darwin, config.toKtorClientConfig()) {

}