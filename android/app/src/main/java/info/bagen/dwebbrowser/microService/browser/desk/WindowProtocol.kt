package info.bagen.dwebbrowser.microService.browser.desk

import io.ktor.http.HttpMethod
import kotlinx.serialization.Serializable
import org.dweb_browser.core.http.bind
import org.dweb_browser.helper.Observable
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.sys.window.core.Rect
import org.dweb_browser.sys.window.core.constant.WindowPropertyKeys
import org.dweb_browser.sys.window.core.constant.WindowStyle
import org.dweb_browser.sys.window.core.constant.debugWindow

suspend fun DeskNMM.windowProtocol(desktopController: DesktopController) {
  protocol("window.sys.dweb") {
    routes(
      /// 打开主窗口，获取主窗口句柄
      // TODO 这样需要跳出授权窗口，获得OTP（一次性密钥），然后在让 desk.browser.dweb 打开窗口
      "/openMainWindow" bind HttpMethod.Get to defineStringResponse {
        openOrActivateAppWindow(desktopController, ipc.remote.mmid)
      },
      "/createBottomSheets" bind HttpMethod.Get to defineJsonResponse {
        createBottomSheetsModal(ipc.remote.mmid).toJsonElement()
      },
      "/createAlert" bind HttpMethod.Get to defineJsonResponse {
        createAlertModal(ipc.remote.mmid).toJsonElement()
      },
      "/openModal" bind HttpMethod.Get to defineBooleanResponse {
        getAppMainWindow(ipc.remote.mmid).openModal(request.query("modalId"))
      },
      "/closeModal" bind HttpMethod.Get to defineBooleanResponse {
        getAppMainWindow(ipc.remote.mmid).closeModal(request.query("modalId"))
      },
      "/removeModal" bind HttpMethod.Get to defineBooleanResponse {
        getAppMainWindow(ipc.remote.mmid).removeModal(request.query("modalId"))
      },
      /** 窗口的状态监听 */
      "/observe" bind HttpMethod.Get to defineJsonLineResponse {
        val win = getWindow()
        debugWindow("/observe", "wid: ${win.id} ,mmid: ${ipc.remote.mmid}")
        win.state.observable.onChange {
          try {
            emit(win.state.toJsonElement())
          } catch (e: Exception) {
            e.printStackTrace()
            end()
          }
        }.also {
          it.removeWhen(onDispose)
          it.emitSelf(
            Observable.Change(
              WindowPropertyKeys.Constants, null, null
            )
          )
        }
      },
      "/getState" bind HttpMethod.Get to defineJsonResponse {
        getWindow().state.toJsonElement()
      },
      "/focus" bind HttpMethod.Get to defineEmptyResponse { getWindow().focus() },
      "/blur" bind HttpMethod.Get to defineEmptyResponse { getWindow().blur() },
      "/maximize" bind HttpMethod.Get to defineEmptyResponse { getWindow().maximize() },
      "/unMaximize" bind HttpMethod.Get to defineEmptyResponse { getWindow().unMaximize() },
      "/visible" bind HttpMethod.Get to defineEmptyResponse { getWindow().toggleVisible() },
      "/close" bind HttpMethod.Get to defineEmptyResponse { getWindow().close() },
      "/setStyle" bind HttpMethod.Get to defineEmptyResponse {
        getWindow().setStyle(request.queryAs<WindowStyle>())
      },
      "/display" bind HttpMethod.Get to defineJsonResponse {
        val manager =
          getWindow().manager ?: return@defineJsonResponse "not found window".toJsonElement()
        val state = manager.state

        @Serializable
        data class Display(
          val height: Float, val width: Float, val imeBoundingRect: Rect
        )
        Display(state.viewHeightDp, state.viewWidthDp, state.imeBoundingRect).toJsonElement()
      })
  }
}