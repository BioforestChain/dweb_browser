package org.dweb_browser.browser.mwebview

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.NativeMicroModule
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
        // 关闭app
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
        "/openOrActivate" bind PureMethod.GET by defineEmptyResponse {
          val url = request.query("url")
          val wid = request.query("wid")

          val remoteMm = getRemoteRuntime()
          debugMM("/openOrActivate", "MultiWebViewNMM open!!! ${remoteMm.mmid}")
          openOrActivity(remoteMm, wid, url, ipc)
        },
      )
    }

    override suspend fun _shutdown() {
    }

    private val openLock = Mutex()

    private suspend fun getController(
      remoteMm: MicroModule.Runtime,
      wid: UUID,
      ipc: Ipc,
    ) = openLock.withLock(remoteMm.mmid) {
      controllerMap.getOrPut(remoteMm) {
        val win = remoteMm.getWindow(wid)
        /// 窗口销毁的时候，释放这个Controller
        /// 通常这里无法正确执行，好像是因为 onClose 是在生命周期之前就已经被释放了？所以目前使用 WeakHashMap
        win.onClose {
          controllerMap.remove(remoteMm)
        }
        win.setStateFromManifest(remoteMm)
        MultiWebViewController(win, ipc, this, remoteMm).also { controller ->
          ipc.onClosed {
            scopeLaunch(cancelable = false) {
              debugMM("/onClosed", "listen ipc close destroy window")
              controller.destroyWebView()
            }
          }
        }
      }
    }

    /**打开新图层*/
    private suspend fun openDwebView(
      remoteMm: MicroModule.Runtime,
      wid: UUID,
      url: String,
      ipc: Ipc,
    ): MultiWebViewController.MultiViewItem {
      debugMM("openDwebView-start") { "remote-mmid: ${remoteMm.mmid} / url:$url" }
      return getController(remoteMm, wid, ipc).openWebView(url).also {
        debugMM("openDwebView-end", it)
      }
    }

    private suspend fun openOrActivity(
      remoteMm: MicroModule.Runtime,
      wid: UUID,
      url: String,
      ipc: Ipc,
    ) {
      val controller = getController(remoteMm, wid, ipc)
      if (controller.webViewList.isEmpty()) {
        debugMM("openOrActivity-start") { "remote-mmid: ${remoteMm.mmid} / url:$url" }
        controller.openWebView(url).also {
          debugMM("openOrActivity-end", it)
        }
      } else {
        controller.win.focus()
      }
    }

    private suspend fun closeDwebView(remoteMm: MicroModule.Runtime, webviewId: String): Boolean {
      return controllerMap[remoteMm]?.closeWebView(webviewId) ?: false
    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) =
    MultiWebViewRuntime(bootstrapContext)
}