package org.dweb_browser.sys.toast

import io.ktor.http.HttpMethod
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule

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
        showToast(message, durationType, positionType)
        return@defineBooleanResponse true
      },
    ).cors()
  }

  override suspend fun _shutdown() {

  }
}

enum class EToast(val type: String) {
  Long("long"), Short("short")
}

enum class DurationType(duration: Long) {
  SHORT(2000L), LONG(3500L)
}

enum class PositionType(val position: String) {
  TOP("top"), CENTER("center"), BOTTOM("bottom")
}