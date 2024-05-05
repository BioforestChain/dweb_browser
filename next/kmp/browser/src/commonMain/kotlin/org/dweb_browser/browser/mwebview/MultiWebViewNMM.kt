package org.dweb_browser.browser.mwebview

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.DisplayMode
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.get
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.ext.getWindow

class MultiWebViewNMM : NativeMicroModule("mwebview.browser.dweb", "Multi Webview Renderer") {
  init {
    short_name = "MWebview"
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Render_Service)
  }

  companion object {
    private val controllerMap = WeakHashMap<MicroModule.Runtime, MultiWebViewController>()
  }

  inner class MultiWebViewRuntime(override val bootstrapContext: BootstrapContext) :
    NativeRuntime() {
    override suspend fun _bootstrap() {
      webViewSysProtocol()

      routes(
        // 打开一个 webview，并将它以 窗口window 的标准进行展示
        "/open" bind PureMethod.GET by defineJsonResponse {
          val url = request.query("url")
          val wid = request.query("wid")

          val remoteMm = getRemoteRuntime()
          debugMM("/open", "MultiWebViewNMM open!!! ${remoteMm.mmid}")
          ipc.onClosed {
            scopeLaunch(cancelable = false) {
              debugMM("/open", "listen ipc close destroy window")
              val controller = controllerMap[remoteMm]
              controller?.destroyWebView()
            }
          }
          val viewLayer = openDwebView(remoteMm, wid, url, ipc)
          debugMM(
            "create/open end",
            "${viewLayer.webviewId}, ${viewLayer.windowController.id}"
          )
          viewLayer.layerController.getState()
        },
        // 关闭指定 webview 窗口
        "/close" bind PureMethod.GET by defineBooleanResponse {
          val webviewId = request.query("webview_id")
          val remoteMmid = ipc.remote.mmid
          debugMM("/close", "webviewId:$webviewId,mmid:$remoteMmid")
          closeDwebView(getRemoteRuntime(), webviewId)
        },
        "/close/app" bind PureMethod.GET by defineBooleanResponse {
          val controller = controllerMap[getRemoteRuntime()] ?: return@defineBooleanResponse false;
          controller.destroyWebView()
        },
        // 界面没有关闭，用于重新唤醒
        "/activate" bind PureMethod.GET by defineBooleanResponse {
          val controller = controllerMap[getRemoteRuntime()] ?: return@defineBooleanResponse false
          debugMM("/activate", "激活 ${controller.ipc.remote.mmid}")
          controller.win.focus()

          return@defineBooleanResponse true
        },
      )
    }

    override suspend fun _shutdown() {
    }

    private val openLock = Mutex()
    private suspend fun openDwebView(
      remoteMm: MicroModule.Runtime,
      wid: UUID,
      url: String,
      ipc: Ipc,
    ) = openLock.withLock(remoteMm.mmid) {
      val remoteMmid = remoteMm.mmid
      debugMM("openDwebView-start") { "remote-mmid: $remoteMmid / url:$url" }

      val controller = controllerMap.getOrPut(remoteMm) {
        val win = remoteMm.getWindow(wid)
        remoteMm.display?.let { mode ->
          if (mode == DisplayMode.Fullscreen) {
            win.maximize()
          }
        }
        /// 窗口销毁的时候，释放这个Controller
        /// 通常这里无法正确执行，好像是因为 onClose 是在生命周期之前就已经被释放了？所以目前使用 WeakHashMap
        win.onClose {
          controllerMap.remove(remoteMm)
        }
        win.setStateFromManifest(remoteMm)
        MultiWebViewController(win, ipc, this, remoteMm)
      }

      controller.openWebView(url).also {
        debugMM("openDwebView-end", it)
      }
    }

    private suspend fun closeDwebView(remoteMm: MicroModule.Runtime, webviewId: String): Boolean {
      return controllerMap[remoteMm]?.closeWebView(webviewId) ?: false
    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) =
    MultiWebViewRuntime(bootstrapContext)
}