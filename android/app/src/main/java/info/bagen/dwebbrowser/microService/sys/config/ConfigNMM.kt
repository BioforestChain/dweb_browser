package info.bagen.dwebbrowser.microService.sys.config

import org.dweb_browser.browserUI.microService.browser.web.debugBrowser
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

class ConfigNMM: NativeMicroModule("config.sys.dweb", "Device Info")  {
  val queryLang = Query.string().required("lang")
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    apiRouting = routes(
      "/setLang" bind Method.GET to defineHandler { request,ipc ->
        debugBrowser("ConfigNMM setLang", request.uri)
        val lang = queryLang(request)
        ConfigStore.set("${ConfigStore.Config}.${ipc.remote.mmid}",lang)
        return@defineHandler true
      },
      "/getLang" bind Method.GET to defineHandler { request,ipc ->
        debugBrowser("getLang", request.uri)
        val lang = ConfigStore.get("${ConfigStore.Config}.${ipc.remote.mmid}")
        return@defineHandler Response(Status.OK).body(lang)
      }
    )
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }
}