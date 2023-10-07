package org.dweb_browser.browser.mwebview

import io.ktor.http.HttpMethod
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.printDebug
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.types.MMID
import org.dweb_browser.microservice.http.bind
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.window.core.WindowState
import org.dweb_browser.window.core.constant.WindowConstants
import org.dweb_browser.window.core.createWindowAdapterManager
import org.dweb_browser.window.core.helper.setFromManifest
import org.dweb_browser.window.render.emitFocusOrBlur

fun debugMultiWebView(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("mwebview", tag, msg, err)

class MultiWebViewNMM : NativeMicroModule("mwebview.browser.dweb", "Multi Webview Renderer") {
  init {
    short_name = "MWebview"
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Render_Service)
  }

  companion object {
    private val controllerMap = mutableMapOf<MMID, MultiWebViewController>()
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    // 打开webview

    routes(
      // 打开一个 webview，并将它以 窗口window 的标准进行展示
      "/open" bind HttpMethod.Get to defineJsonResponse {
        val url = request.query("url")
        debugMultiWebView("create/open", url)
        val remoteMm = ipc.asRemoteInstance()
          ?: throw Exception("mwebview.browser.dweb/open should be call by locale")
        ipc.onClose {
          debugMultiWebView("/open", "listen ipc close destroy window")
          val controller = controllerMap[ipc.remote.mmid]
          controller?.destroyWebView()
        }
        debugMultiWebView("/open", "MultiWebViewNMM open!!! ${remoteMm.mmid}")

        val (viewItem, controller) = openDwebView(url, remoteMm, ipc)
        debugMultiWebView("create/open end", "${viewItem.webviewId}, ${controller.win.id}")
        controller.getState()
      },
      // 关闭指定 webview 窗口
      "/close" bind HttpMethod.Get to defineBooleanResponse {
        val webviewId = request.query("webview_id")
        val remoteMmid = ipc.remote.mmid
        debugMultiWebView("/close", "webviewId:$webviewId,mmid:$remoteMmid")
        closeDwebView(remoteMmid, webviewId)
      },
      "/close/app" bind HttpMethod.Get to defineBooleanResponse {
        val controller = controllerMap[ipc.remote.mmid] ?: return@defineBooleanResponse false;
        controller.destroyWebView()
      },
      // 界面没有关闭，用于重新唤醒
      "/activate" bind HttpMethod.Get to defineBooleanResponse {
        val remoteMmid = ipc.remote.mmid
        val controller = controllerMap[remoteMmid] ?: return@defineBooleanResponse false
        debugMultiWebView("/activate", "激活 ${controller.ipc.remote.mmid}")
        controller.win.state.apply {
          focus = true
          visible = true
        }
        // TODO 将当前的界面移动到最上层
//        controller.ipc.postMessage(IpcEvent.fromUtf8(EIpcEvent.Activity.event, ""))
        controller.win.emitFocusOrBlur(true)

        return@defineBooleanResponse true
      },
    )
  }

  @Serializable
  data class ViewItemResponse(val webviewId: String, val wid: UUID)

  override suspend fun _shutdown() {

  }

  private val openLock = Mutex()
  private suspend fun openDwebView(
    url: String,
    remoteMm: MicroModule,
    ipc: Ipc,
  ) = openLock.withLock(remoteMm.mmid) {
    val remoteMmid = remoteMm.mmid
    debugMultiWebView("/open", "remote-mmid: $remoteMmid / url:$url")

    val controller = controllerMap.getOrPut(remoteMmid) {
      val win = createWindowAdapterManager.createWindow(WindowState(
        WindowConstants(
          owner = ipc.remote.mmid,
          ownerVersion = ipc.remote.version,
          provider = mmid,
          microModule = this
        )
      ).apply { setFromManifest(ipc.remote) })

      MultiWebViewController(win, ipc, remoteMm, this).also { controller ->
        /// 窗口销毁的时候，释放这个Controller
        win.onClose {
          controllerMap.remove(remoteMmid)
        }
//        ioAsyncScope.launch {
//          controller.downLoadObserver = DownLoadObserver(remoteMmid).apply {
//            observe { listener ->
//              controller.lastViewOrNull?.webView?.let { dWebView ->
//                emitEvent(
//                  dWebView, listener.downLoadStatus.toServiceWorkerEvent(), listener.progress
//                )
//              }
//            }
//          }
//        }
      }
    }

    Pair(controller.openWebView(url), controller)
  }

  private suspend fun closeDwebView(remoteMmid: String, webviewId: String): Boolean {
    return controllerMap[remoteMmid]?.closeWebView(webviewId) ?: false
  }
}