package info.bagen.dwebbrowser.microService.browser.desk

import io.ktor.http.HttpMethod
import org.dweb_browser.core.http.bind
import org.dweb_browser.helper.Observable
import org.dweb_browser.helper.toJsonElement
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
      "/openBottomSheets" bind HttpMethod.Get to defineStringResponse {
//        nativeFetch("file://desk.browser.dweb/open")
        ""
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
    )
  }
}