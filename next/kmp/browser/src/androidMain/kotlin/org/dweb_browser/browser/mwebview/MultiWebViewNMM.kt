package org.dweb_browser.browser.mwebview

import io.ktor.http.HttpMethod
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.bind
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.DisplayMode
import org.dweb_browser.helper.printDebug
import org.dweb_browser.sys.window.ext.openMainWindow
import org.dweb_browser.sys.window.render.emitFocusOrBlur

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
    webViewSysProtocol()

    routes(
      // 打开一个 webview，并将它以 窗口window 的标准进行展示
      "/open" bind HttpMethod.Get to defineJsonResponse {
        val url = request.query("url")

        val remoteMm = ipc.asRemoteInstance()
          ?: throw Exception("mwebview.browser.dweb/open should be call by locale")
        debugMultiWebView("/open", "MultiWebViewNMM open!!! ${remoteMm.mmid}")
        ipc.onClose {
          debugMultiWebView("/open", "listen ipc close destroy window")
          val controller = controllerMap[ipc.remote.mmid]
          controller?.destroyWebView()
        }
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
        controller.win.emitFocusOrBlur(true)

        return@defineBooleanResponse true
      },
    )
  }

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
      val win =
        remoteMm.openMainWindow() ?: throw Exception("fail to got window for ${remoteMm.mmid}")
      remoteMm.manifest.display?.let { mode ->
        if (mode == DisplayMode.Fullscreen) {
          win.maximize()
        }
      }
      /// 窗口销毁的时候，释放这个Controller
      win.onClose {
        controllerMap.remove(remoteMmid)
      }
      MultiWebViewController(win, ipc, remoteMm, this)
    }

    Pair(controller.openWebView(url), controller)
  }

  private suspend fun closeDwebView(remoteMmid: String, webviewId: String): Boolean {
    return controllerMap[remoteMmid]?.closeWebView(webviewId) ?: false
  }
}