package info.bagen.dwebbrowser.microService.browser.nativeui.statusBar

import org.dweb_browser.microservice.help.Mmid
import info.bagen.dwebbrowser.microService.browser.nativeui.NativeUiController
import info.bagen.dwebbrowser.microService.browser.nativeui.helper.fromMultiWebView
import info.bagen.dwebbrowser.microService.browser.nativeui.helper.QueryHelper
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.utils.io.write
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.ipc.helper.IpcResponse
import org.http4k.core.BodyMode
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream

class StatusBarNMM : NativeMicroModule("status-bar.nativeui.browser.dweb") {

  private fun getController(mmid: Mmid) =
    NativeUiController.fromMultiWebView(mmid).statusBar

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val queryMMid = Query.string().required("mmid")
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
      "/observe" bind Method.GET to defineHandler { request, ipc ->
        val outputStream = ByteArrayOutputStream()
        val mmid = queryMMid(request)
        getController(mmid).observer.observe { state ->
          outputStream.write(state.toByteArray())
        }
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        return@defineHandler Response(Status.OK).body(inputStream)
      },
    )
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }
}
