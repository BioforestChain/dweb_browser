@file:JsModule("./http-server.mjs")

package org.dweb_browser.wasm_backend

import kotlin.js.Promise

external class ReqResCtx {
  val url: String
  fun setHeader(key: JsString, value: JsString)
  fun write(data: JsAny)
  fun end()
}

external fun createHttpServer(
  port: JsNumber = definedExternally,
  onReq: (ReqResCtx) -> Unit
): Promise<JsNumber>