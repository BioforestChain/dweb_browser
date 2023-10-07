package info.bagen.dwebbrowser.microService.sys.toast

import info.bagen.dwebbrowser.microService.sys.toast.ToastController.DurationType
import info.bagen.dwebbrowser.microService.sys.toast.ToastController.PositionType
import io.ktor.http.HttpMethod
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.http.bind

class ToastNMM : NativeMicroModule("toast.sys.dweb", "toast") {

  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Render_Service);
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      /** 显示弹框*/
      "/show" bind HttpMethod.Get to defineBooleanResponse {
        val duration = request.queryOrNull("duration") ?: EToast.Short.type
        val message = request.query("message")
        val position = request.queryOrNull("position") ?: PositionType.BOTTOM.position
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
        return@defineBooleanResponse true
      },
    ).cors()
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }
}

enum class EToast(val type: String) {
  Long("long"), Short("short")
}