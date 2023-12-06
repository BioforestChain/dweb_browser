package org.dweb_browser.browser.nativeui.safeArea

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

class SafeAreaNMM : NativeMicroModule("safe-area.nativeui.browser.dweb", "safeArea") {

  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Render_Service);
  }

  private fun getController(mmid: MMID) = NativeUiController.fromMultiWebView(mmid).safeArea

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      /** 获取状态 */
      "/getState" bind HttpMethod.Get by defineJsonResponse {
        return@defineJsonResponse getController(ipc.remote.mmid).toJsonElement()
      },
      /** 获取状态 */
      "/setState" bind HttpMethod.Get by defineEmptyResponse {
        val controller = getController(ipc.remote.mmid)
        QueryHelper.overlay(request)?.also { controller.overlayState.value = it }
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
  }
}
