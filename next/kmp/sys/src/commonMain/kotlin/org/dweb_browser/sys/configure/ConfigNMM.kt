package org.dweb_browser.sys.configure

import io.ktor.http.HttpStatusCode
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureStringBody

val debugConfig = Debugger("config")

class ConfigNMM : NativeMicroModule("config.sys.dweb", "Device Info") {

  inner class ConfigRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    override suspend fun _bootstrap() {
      val store = ConfigStore(this)
      routes(
        "/setLang" bind PureMethod.GET by defineBooleanResponse {
          debugConfig("ConfigNMM setLang", request.href)
          val lang = request.query("lang")
          store.set("language.${ipc.remote.mmid}", lang)
          return@defineBooleanResponse true
        },
        "/getLang" bind PureMethod.GET by definePureResponse {
          debugConfig("getLang", request.href)
          val lang = store.get("language.${ipc.remote.mmid}")
          return@definePureResponse PureResponse(
            HttpStatusCode.OK,
            body = PureStringBody(lang ?: "")
          )
        }
      )
    }

    override suspend fun _shutdown() {

    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = ConfigRuntime(bootstrapContext)
}