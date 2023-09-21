package info.bagen.dwebbrowser.microService.sys.toast

import info.bagen.dwebbrowser.microService.sys.toast.ToastController.DurationType
import info.bagen.dwebbrowser.microService.sys.toast.ToastController.PositionType
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.cors
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.http4k.core.Method
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

class ToastNMM : NativeMicroModule("toast.sys.dweb", "toast") {

  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Render_Service);
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    apiRouting = routes(
      /** 显示弹框*/
      "/show" bind Method.GET to defineHandler { request ->
        val duration = Query.string().defaulted("duration", EToast.Short.type)(request)
        val message = Query.string().required("message")(request)
        val position = Query.string().defaulted("position", PositionType.BOTTOM.position)(request)
        val durationType = when (duration) {
          EToast.Long.type -> DurationType.LONG
          else -> DurationType.SHORT
        }
        val positionType = when (position) {
          PositionType.BOTTOM.position -> PositionType.BOTTOM
          PositionType.CENTER.position -> PositionType.CENTER
          else -> PositionType.TOP
        }
        ToastController.show(message, durationType, positionType)
        return@defineHandler true
      },
    ).cors()
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }
}

enum class EToast(val type: String) {
  Long("long"),
  Short("short")
}