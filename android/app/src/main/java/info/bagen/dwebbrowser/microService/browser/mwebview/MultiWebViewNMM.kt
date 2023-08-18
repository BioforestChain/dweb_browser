package info.bagen.dwebbrowser.microService.browser.mwebview

import org.dweb_browser.helper.ComparableWrapper
import org.dweb_browser.helper.enumToComparable
import info.bagen.dwebbrowser.microService.core.AndroidNativeMicroModule
import info.bagen.dwebbrowser.microService.core.UUID
import info.bagen.dwebbrowser.microService.core.WindowState
import info.bagen.dwebbrowser.microService.core.windowAdapterManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.dweb_browser.browserUI.download.DownLoadObserver
import org.dweb_browser.dwebview.base.ViewItem
import org.dweb_browser.dwebview.serviceWorker.emitEvent
import org.dweb_browser.helper.ImageResourcePurposes
import org.dweb_browser.helper.StrictImageResource
import org.dweb_browser.helper.printdebugln
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.MMID
import org.dweb_browser.microservice.ipc.Ipc
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

fun debugMultiWebView(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("mwebview", tag, msg, err)

class MultiWebViewNMM :
  AndroidNativeMicroModule("mwebview.browser.dweb", "Multi Webview Renderer") {
  override val short_name = "MWebview";
  override val categories =
    mutableListOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Render_Service);

  companion object {
    private val controllerMap = mutableMapOf<MMID, MultiWebViewController>()

    /**获取当前的controller, 只能给nativeUI 使用，因为他们是和mwebview绑定在一起的
     */
    fun getCurrentWebViewController(mmid: MMID): MultiWebViewController? {
      return controllerMap[mmid]
    }
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    // 打开webview
    val queryUrl = Query.string().required("url")
    val queryWebviewId = Query.string().required("webview_id")

    apiRouting = routes(
      // 打开一个 webview，并将它以 窗口window 的标准进行展示
      "/open" bind Method.GET to defineHandler { request, ipc ->
        val url = queryUrl(request)
        val remoteMm = ipc.asRemoteInstance()
          ?: throw Exception("mwebview.browser.dweb/open should be call by locale")
        ipc.onClose {
          debugMultiWebView("/open", "listen ipc close destroy window")
          val controller = controllerMap[ipc.remote.mmid]
          controller?.destroyWebView()
        }

        val (viewItem, controller) = openDwebView(url, remoteMm, ipc)
        return@defineHandler ViewItemResponse(viewItem.webviewId, controller.win.id)
      },
      // 关闭指定 webview 窗口
      "/close" bind Method.GET to defineHandler { request, ipc ->
        val webviewId = queryWebviewId(request)
        val remoteMmid = ipc.remote.mmid
        debugMultiWebView("/close", "webviewId:$webviewId,mmid:$remoteMmid")
        closeDwebView(remoteMmid, webviewId)
      },
      "/close/app" bind Method.GET to defineHandler { request, ipc ->
        val controller = controllerMap[ipc.remote.mmid] ?: return@defineHandler false;
        controller.destroyWebView()
      },
      // 界面没有关闭，用于重新唤醒
      "/activate" bind Method.GET to defineHandler { request, ipc ->
        val remoteMmid = ipc.remote.mmid
        val controller = controllerMap[remoteMmid] ?: return@defineHandler false;
        debugMultiWebView("/activate", "激活 ${controller.ipc.remote.mmid}")
        // TODO 将当前的界面移动到最上层
        //  controller.ipc.postMessage(IpcEvent.fromUtf8(EIpcEvent.Activity.event, ""))
        return@defineHandler Response(Status.OK)
      },
    )
  }

  data class ViewItemResponse(val webviewId: String, val wid: UUID)

  override suspend fun _shutdown() {
    apiRouting = null
  }

  private suspend fun openDwebView(
    url: String,
    remoteMm: MicroModule,
    ipc: Ipc,
  ): Pair<ViewItem, MultiWebViewController> {
    val remoteMmid = remoteMm.mmid
    debugMultiWebView("/open", "remote-mmid: $remoteMmid / url:$url")

    val controller = controllerMap.getOrPut(remoteMmid) {
      val win = windowAdapterManager.createWindow(
        WindowState(owner = ipc.remote.mmid, provider = mmid, microModule = this).also { state ->
          /**
           * 挑选合适的图标作为应用的图标
           */
          val iconResource = ipc.remote.icons?.let { icons ->
            val comparableBuilder =
              ComparableWrapper.Builder<StrictImageResource> { imageResource ->
                mapOf(
                  "purpose" to enumToComparable(
                    imageResource.purpose,
                    listOf(
                      ImageResourcePurposes.Maskable,
                      ImageResourcePurposes.Any,
                      ImageResourcePurposes.Monochrome
                    )
                  ).first(),
                  "type" to enumToComparable(
                    imageResource.type,
                    listOf("image/svg+xml", "image/png", "image/jpeg", "image/*")
                  ),
                  "area" to imageResource.sizes.last().let {
                    -it.width * it.height
                  }
                )
              }

            icons.minOfOrNull { comparableBuilder.build(StrictImageResource.from(it)) }?.value
          }
          if (iconResource != null) {
            state.iconUrl = iconResource.src
            state.iconMaskable = iconResource.purpose.contains(ImageResourcePurposes.Maskable)
            state.iconMonochrome = iconResource.purpose.contains(ImageResourcePurposes.Maskable)
          } else {

          }
        }
      )

      MultiWebViewController(win, ipc, remoteMm, this).also { controller ->
        /// 窗口销毁的时候，释放这个Controller
        win.onClose {
          controllerMap.remove(remoteMmid)
        }
        GlobalScope.launch {
          controller.downLoadObserver = DownLoadObserver(remoteMmid).apply {
            observe { listener ->
              controller.lastViewOrNull?.webView?.let { dWebView ->
                emitEvent(
                  dWebView, listener.downLoadStatus.toServiceWorkerEvent(), listener.progress
                )
              }
            }
          }
        }
      }
    }

    return Pair(controller.openWebView(url), controller)
  }

  private suspend fun closeDwebView(remoteMmid: String, webviewId: String): Boolean {
    return controllerMap[remoteMmid]?.closeWebView(webviewId) ?: false
  }
}