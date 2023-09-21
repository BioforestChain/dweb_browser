package info.bagen.dwebbrowser.microService.browser.nativeui.virtualKeyboard

import info.bagen.dwebbrowser.microService.browser.nativeui.NativeUiController
import info.bagen.dwebbrowser.microService.browser.nativeui.helper.QueryHelper
import info.bagen.dwebbrowser.microService.browser.nativeui.helper.debugNativeUi
import info.bagen.dwebbrowser.microService.browser.nativeui.helper.fromMultiWebView
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.cors
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.types.MMID
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes

class VirtualKeyboardNMM :
  NativeMicroModule("virtual-keyboard.nativeui.browser.dweb", "virtualKeyBoard") {

  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Render_Service);
  }

  private fun getController(mmid: MMID) =
    NativeUiController.fromMultiWebView(mmid).virtualKeyboard

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    QueryHelper.init()
    apiRouting = routes(
      /** 获取状态 */
      "/getState" bind Method.GET to defineHandler { _, ipc ->
        val controller = getController(ipc.remote.mmid);
        debugNativeUi("virtual-keyboard getState", controller.overlayState.value)
        return@defineHandler controller
      },
      /** 设置状态 */
      "/setState" bind Method.GET to defineHandler { request, ipc ->
        val controller = getController(ipc.remote.mmid)
        QueryHelper.overlay(request)?.also { controller.overlayState.value = it }
        QueryHelper.visible(request)?.also { controller.visibleState.value = it }
        return@defineHandler null
      },
      /**
       * 开始数据订阅
       */
      "/observe" bind Method.GET to defineHandler { _, ipc ->
        val inputStream = getController(ipc.remote.mmid).observer.startObserve(ipc)
        return@defineHandler Response(Status.OK).body(inputStream)
      },
    ).cors()
  }

  override suspend fun _shutdown() {
  }
}
