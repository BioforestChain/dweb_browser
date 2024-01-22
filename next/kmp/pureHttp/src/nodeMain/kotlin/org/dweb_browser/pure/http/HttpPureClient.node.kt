package org.dweb_browser.pure.http

import org.dweb_browser.pure.http.ktor.KtorPureClient

actual class HttpPureClient : KtorPureClient(io.ktor.client.engine.js.Js) {

}