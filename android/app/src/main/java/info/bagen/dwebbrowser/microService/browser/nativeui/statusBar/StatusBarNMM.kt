package info.bagen.dwebbrowser.microService.browser.nativeui.statusBar

import info.bagen.dwebbrowser.microService.browser.nativeui.NativeUiController
import info.bagen.dwebbrowser.microService.browser.nativeui.helper.QueryHelper
import info.bagen.dwebbrowser.microService.browser.nativeui.helper.fromMultiWebView
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.MMID
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.cors
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes

class StatusBarNMM : NativeMicroModule("status-bar.nativeui.browser.dweb", "statusBar") {

  override val categories =
    mutableListOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Render_Service)

  private fun getController(mmid: MMID) =
    NativeUiController.fromMultiWebView(mmid).statusBar

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    apiRouting = routes(
      /** 获取状态栏 */
      "/getState" bind Method.GET to defineHandler { _, ipc ->
        return@defineHandler getController(ipc.remote.mmid)
      },
      /** 设置状态栏 */
      "/setState" bind Method.GET to defineHandler { request, ipc ->
        val controller = getController(ipc.remote.mmid)
        QueryHelper.color(request)?.also { controller.colorState.value = it }
        QueryHelper.style(request)?.also { controller.styleState.value = it }
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
      /**
       * 关闭订阅数据流
       */
      "/stopObserve" bind Method.GET to defineHandler { _, ipc ->
        return@defineHandler getController(ipc.remote.mmid).observer.stopObserve(ipc)
      },
    ).cors()
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }
}
