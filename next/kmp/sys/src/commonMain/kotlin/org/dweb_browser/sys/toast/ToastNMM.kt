package org.dweb_browser.sys.toast

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.pure.http.PureMethod


class ToastNMM : NativeMicroModule("toast.sys.dweb", "toast") {

  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Render_Service);
  }

  inner class ToastRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {

    override suspend fun _bootstrap() {
      routes(
        /** 显示弹框*/
        "/show" bind PureMethod.GET by defineBooleanResponse {
          val duration =
            request.queryOrNull("duration")?.let { ToastDurationType.ALL[it] }
              ?: ToastDurationType.Default
          val message = request.query("message")
          val position =
            request.queryOrNull("position")?.let { ToastPositionType.ALL[it] }
              ?: ToastPositionType.Default

          val fromMM = getRemoteRuntime()
          debugMM("show-toast") {
            "message=$message,duration=${duration},position=${position}"
          }
          showToast(fromMM, message, duration, position)
          return@defineBooleanResponse true
        },
      ).cors()
    }

    override suspend fun _shutdown() {

    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = ToastRuntime(bootstrapContext)

}

enum class ToastDurationType(val type: String, val duration: Long) {
  SHORT("short", 2000L), LONG("long", 3500L),
  ;

  companion object {
    val ALL = entries.associateBy { it.type }
    val Default = SHORT
  }
}

enum class ToastPositionType(val position: String) {
  TOP("top"), CENTER("center"), BOTTOM("bottom"),
  ;

  companion object {
    val ALL = ToastPositionType.entries.associateBy { it.position }
    val Default = BOTTOM
  }
}