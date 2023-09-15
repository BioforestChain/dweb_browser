package info.bagen.dwebbrowser.microService.sys.config

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import org.dweb_browser.browserUI.microService.browser.web.debugBrowser
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.http.PureStringBody
import org.dweb_browser.microservice.http.bind

class ConfigNMM: NativeMicroModule("config.sys.dweb", "Device Info")  {
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      "/setLang" bind HttpMethod.Get to defineBooleanResponse {
        debugBrowser("ConfigNMM setLang", request.href)
        val lang = request.queryOrFail("lang")
        ConfigStore.set("${ConfigStore.Config}.${ipc.remote.mmid}",lang)
        return@defineBooleanResponse true
      },
      "/getLang" bind HttpMethod.Get to definePureResponse {
        debugBrowser("getLang", request.href)
        val lang = ConfigStore.get("${ConfigStore.Config}.${ipc.remote.mmid}")
        return@definePureResponse PureResponse(HttpStatusCode.OK, body = PureStringBody(lang))
      }
    )
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }
}