package org.dweb_browser.sys.configure

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.PureStringBody
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger

val debugConfig = Debugger("config")

class ConfigNMM : NativeMicroModule("config.sys.dweb", "Device Info") {
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val store = ConfigStore(this)
    routes(
      "/setLang" bind HttpMethod.Get to defineBooleanResponse {
        debugConfig("ConfigNMM setLang", request.href)
        val lang = request.query("lang")
        store.set("language.${ipc.remote.mmid}", lang)
        return@defineBooleanResponse true
      },
      "/getLang" bind HttpMethod.Get to definePureResponse {
        debugConfig("getLang", request.href)
        val lang = store.get("language.${ipc.remote.mmid}")
        return@definePureResponse PureResponse(HttpStatusCode.OK, body = PureStringBody(lang))
      }
    )
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }
}