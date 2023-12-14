package org.dweb_browser.browser.desk

import io.ktor.http.HttpMethod
import kotlinx.serialization.Serializable
import org.dweb_browser.core.http.queryAs
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.helper.Observable
import org.dweb_browser.helper.Rect
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.sys.window.core.constant.WindowPropertyKeys
import org.dweb_browser.sys.window.core.constant.WindowStyle
import org.dweb_browser.sys.window.core.constant.debugWindow

suspend fun DeskNMM.windowProtocol(desktopController: DesktopController) {
  protocol("window.sys.dweb") {
    routes(
      /// 打开主窗口，获取主窗口句柄
      // TODO 这样需要跳出授权窗口，获得OTP（一次性密钥），然后在让 desk.browser.dweb 打开窗口
      "/openMainWindow" bind HttpMethod.Get by defineStringResponse {
        openOrActivateAppWindow(ipc, desktopController).id
      },
      "/mainWindow" bind HttpMethod.Get by defineStringResponse {
        getAppMainWindow().id
      },
      "/createModal" bind HttpMethod.Get by defineJsonResponse {
        createModal(ipc).toJsonElement()
      },
      "/openModal" bind HttpMethod.Get by defineBooleanResponse {
        getAppMainWindow().openModal(request.query("modalId"))
      },
      "/updateModalCloseTip" bind HttpMethod.Get by defineBooleanResponse {
        getAppMainWindow().updateModalCloseTip(
          request.query("modalId"),
          request.queryOrNull("closeTip")
        )
      },
      "/closeModal" bind HttpMethod.Get by defineBooleanResponse {
        getAppMainWindow().closeModal(this@windowProtocol, request.query("modalId"))
      },
      "/removeModal" bind HttpMethod.Get by defineBooleanResponse {
        getAppMainWindow().removeModal(this@windowProtocol, request.query("modalId"))
      },
      /** 窗口的状态监听 */
      "/observe" byChannel { ctx ->
        val win = getWindow()
        debugWindow("/observe", "wid: ${win.id} ,mmid: ${ipc.remote.mmid}")
        win.state.observable.onChange {
          try {
            ctx.sendJsonLine(win.state.toJsonElement())
          } catch (e: Exception) {
            e.printStackTrace()
            close(cause = e)
          }
        }.also {
          it.removeWhen(onClose)
          it.emitSelf(
            Observable.Change(
              WindowPropertyKeys.Constants, null, null
            )
          )
        }
      },
      "/getState" bind HttpMethod.Get by defineJsonResponse {
        getWindow().state.toJsonElement()
      },
      "/focus" bind HttpMethod.Get by defineEmptyResponse { getWindow().focus() },
      "/blur" bind HttpMethod.Get by defineEmptyResponse { getWindow().blur() },
      "/maximize" bind HttpMethod.Get by defineEmptyResponse { getWindow().maximize() },
      "/unMaximize" bind HttpMethod.Get by defineEmptyResponse { getWindow().unMaximize() },
      "/visible" bind HttpMethod.Get by defineEmptyResponse { getWindow().toggleVisible() },
      "/close" bind HttpMethod.Get by defineEmptyResponse { getWindow().tryCloseOrHide() },
      "/closeWindow" bind HttpMethod.Get by defineEmptyResponse { getWindow().closeRoot(true) },
      "/setStyle" bind HttpMethod.Get by defineEmptyResponse {
        getWindow().setStyle(request.queryAs<WindowStyle>())
      },
      "/display" bind HttpMethod.Get by defineJsonResponse {
        val manager =
          getWindow().manager ?: return@defineJsonResponse "not found window".toJsonElement()
        val state = manager.state

        @Serializable
        data class Display(
          val height: Float, val width: Float, val imeBoundingRect: Rect
        )
        Display(state.viewHeightDp(), state.viewWidthDp(), state.imeBoundingRect).toJsonElement()
      })
  }
}