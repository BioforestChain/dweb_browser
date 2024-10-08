package org.dweb_browser.browser.desk

import kotlinx.serialization.Serializable
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.helper.Observable
import org.dweb_browser.helper.PureBounds
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.queryAs
import org.dweb_browser.sys.window.core.SetWindowSize
import org.dweb_browser.sys.window.core.constant.WindowPropertyKeys
import org.dweb_browser.sys.window.core.constant.WindowStyle
import org.dweb_browser.sys.window.core.constant.debugWindow

suspend fun DeskNMM.DeskRuntime.windowProtocol() {
  protocol("window.sys.dweb") {
    routes(
      /// 打开主窗口，获取主窗口句柄
      // TODO 这样需要跳出授权窗口，获得OTP（一次性密钥），然后在让 desk.browser.dweb 打开窗口
      "/openMainWindow" bind PureMethod.GET by defineStringResponse {
        openOrActivateAppWindow(ipc, deskController.getDesktopController()).id
      },
      "/mainWindow" bind PureMethod.GET by defineStringResponse {
        getAppMainWindow().id
      },
      "/createModal" bind PureMethod.GET by defineJsonResponse {
        createModal(ipc).toJsonElement()
      },
      "/openModal" bind PureMethod.GET by defineBooleanResponse {
        val controller = getAppMainWindow(ipc)
        controller.openModal(request.query("modalId"))
      },
      "/updateModalCloseTip" bind PureMethod.GET by defineBooleanResponse {
        getAppMainWindow().updateModalCloseTip(
          request.query("modalId"),
          request.queryOrNull("closeTip")
        )
      },
      "/closeModal" bind PureMethod.GET by defineBooleanResponse {
        getAppMainWindow().closeModal(this@windowProtocol, request.query("modalId"))
      },
      "/removeModal" bind PureMethod.GET by defineBooleanResponse {
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
      "/observe-keyboard" byChannel { ctx ->
        val win = getWindow()
        debugWindow("/observe-keyboard", "wid: ${win.id} ,mmid: ${ipc.remote.mmid}")
        @Serializable
        data class KeyboardState(
          val insets: PureBounds,
          val overlay: Boolean,
        )
        win.state.observable.onChange {
          if (it.key == WindowPropertyKeys.KeyboardInsetBottom || it.key == WindowPropertyKeys.KeyboardOverlaysContent) {
            try {
              ctx.sendJsonLine(
                KeyboardState(
                  insets = PureBounds.Zero.copy(bottom = win.state.keyboardInsetBottom),
                  overlay = win.state.keyboardOverlaysContent
                )
              )
            } catch (e: Exception) {
              e.printStackTrace()
              close(cause = e)
            }
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
      "/getState" bind PureMethod.GET by defineJsonResponse {
        getWindow().state.toJsonElement()
      },
      "/focus" bind PureMethod.GET by defineEmptyResponse { getWindow().focus() },
      "/blur" bind PureMethod.GET by defineEmptyResponse { getWindow().blur() },
      "/maximize" bind PureMethod.GET by defineEmptyResponse { getWindow().maximize() },
      "/unMaximize" bind PureMethod.GET by defineEmptyResponse { getWindow().unMaximize() },
      "/visible" bind PureMethod.GET by defineEmptyResponse { getWindow().toggleVisible() },
      "/close" bind PureMethod.GET by defineEmptyResponse { getWindow().tryCloseOrHide() },
      "/closeWindow" bind PureMethod.GET by defineEmptyResponse { getWindow().closeRoot(true) },
      "/setStyle" bind PureMethod.GET by defineEmptyResponse {
        getWindow().setStyle(request.queryAs<WindowStyle>())
      },
      // 设置窗口大小
      "/setBounds" bind PureMethod.GET by defineEmptyResponse {
        getWindow().setBounds(request.queryAs<SetWindowSize>())
      },
      // 获取窗口信息
      "/getDisplayInfo" bind PureMethod.GET by defineJsonResponse {
        val manager =
          getWindow().getManager() ?: throwException(message = "not found window")
        val state = manager.state

        @Serializable
        data class Display(
          val height: Float, val width: Float, val imeBoundingRect: PureRect,
        )

        val displaySize = state.viewBox.getDisplaySize()
        Display(displaySize.height, displaySize.width, state.imeBoundingRect).toJsonElement()
      })
  }
}