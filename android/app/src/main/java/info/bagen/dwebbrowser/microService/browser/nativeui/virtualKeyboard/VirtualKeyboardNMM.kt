package info.bagen.dwebbrowser.microService.browser.nativeui.virtualKeyboard

import info.bagen.dwebbrowser.microService.browser.nativeui.NativeUiController
import info.bagen.dwebbrowser.microService.browser.nativeui.helper.QueryHelper
import info.bagen.dwebbrowser.microService.browser.nativeui.helper.debugNativeUi
import info.bagen.dwebbrowser.microService.browser.nativeui.helper.fromMultiWebView
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.types.MMID
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.http.PureStreamBody
import org.dweb_browser.microservice.http.bind
import org.dweb_browser.microservice.http.routes

class VirtualKeyboardNMM :
  NativeMicroModule("virtual-keyboard.nativeui.browser.dweb", "virtualKeyBoard") {

  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Render_Service);
  }

  private fun getController(mmid: MMID) =
    NativeUiController.fromMultiWebView(mmid).virtualKeyboard

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    QueryHelper.init()
    routes(
      /** 获取状态 */
      "/getState" bind HttpMethod.Get to defineJsonResponse {
        val controller = getController(ipc.remote.mmid);
        debugNativeUi("virtual-keyboard getState", controller.overlayState.value)
        return@defineJsonResponse controller.toJsonElement()
      },
      /** 设置状态 */
      "/setState" bind HttpMethod.Get to defineEmptyResponse {
        val controller = getController(ipc.remote.mmid)
        QueryHelper.overlay(request)?.also { controller.overlayState.value = it }
        QueryHelper.visible(request)?.also { controller.visibleState.value = it }
        return@defineEmptyResponse
      },
      /**
       * 开始数据订阅
       */
      "/observe" bind HttpMethod.Get to definePureResponse {
        val inputStream = getController(ipc.remote.mmid).observer.startObserve(ipc)
        return@definePureResponse PureResponse(
          HttpStatusCode.OK,
          body = PureStreamBody(inputStream)
        )
      },
    )
//      .cors()
  }

  override suspend fun _shutdown() {
  }
}
