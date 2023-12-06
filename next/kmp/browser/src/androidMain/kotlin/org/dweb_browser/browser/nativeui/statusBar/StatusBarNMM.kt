package org.dweb_browser.browser.nativeui.statusBar

import org.dweb_browser.browser.nativeui.helper.QueryHelper
import org.dweb_browser.browser.nativeui.helper.fromMultiWebView
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import org.dweb_browser.browser.nativeui.NativeUiController
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.PureStreamBody
import org.dweb_browser.core.http.router.bind

class StatusBarNMM : NativeMicroModule("status-bar.nativeui.browser.dweb", "statusBar") {

  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Render_Service)
  }

  private fun getController(mmid: MMID) = NativeUiController.fromMultiWebView(mmid).statusBar

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      /** 获取状态栏 */
      "/getState" bind HttpMethod.Get by defineJsonResponse {
        return@defineJsonResponse getController(ipc.remote.mmid).toJsonElement()
      },
      /** 设置状态栏 */
      "/setState" bind HttpMethod.Get by defineEmptyResponse {
        val controller = getController(ipc.remote.mmid)
        QueryHelper.color(request)?.also { controller.colorState.value = it }
        QueryHelper.style(request)?.also { controller.styleState.value = it }
        QueryHelper.overlay(request)?.also { controller.overlayState.value = it }
        QueryHelper.visible(request)?.also { controller.visibleState.value = it }
        return@defineEmptyResponse
      },
      /**
       * 开始数据订阅
       */
      "/observe" bind HttpMethod.Get by definePureResponse {
        val inputStream = getController(ipc.remote.mmid).observer.startObserve(ipc)
        return@definePureResponse PureResponse(
          HttpStatusCode.OK, body = PureStreamBody(inputStream)
        )
      },
    ).cors()
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }
}
