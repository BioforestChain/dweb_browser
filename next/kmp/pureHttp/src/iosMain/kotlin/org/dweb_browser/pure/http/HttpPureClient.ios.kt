package org.dweb_browser.pure.http

import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinClientEngineConfig
import org.dweb_browser.pure.http.ktor.KtorPureClient

actual class HttpPureClient : KtorPureClient<DarwinClientEngineConfig>(Darwin) {

}