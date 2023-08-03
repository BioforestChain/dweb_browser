package info.bagen.dwebbrowser.microService.browser.mwebview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebViewNavigator
import com.google.accompanist.web.WebViewState
import info.bagen.dwebbrowser.microService.browser.nativeui.NativeUiController
import info.bagen.dwebbrowser.microService.core.WindowController
import info.bagen.dwebbrowser.microService.core.windowAdapterManager
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dweb_browser.browserUI.download.DownLoadObserver
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.dwebview.base.ViewItem
import org.dweb_browser.helper.Callback
import org.dweb_browser.helper.ChangeableList
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.helper.IpcEvent
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

/**
 * MWebView 是为其它模块提供 GUI 的程序模块，所以这里需要传入两个模块：localeMM 与 remoteMM
 * remoteMM 只是一层抽象，未来如何需要可以通过网络成实现
 */
@Stable
class MultiWebViewController(
  /**
   * 窗口控制器
   */
  val win: WindowController,
  /**
   * 控制者的通讯通道
   */
  val ipc: Ipc,
  /// 以下这两参数是用来构建DWebView的时候使用的
  private val localeMM: MicroModule,
  private val remoteMM: MicroModule,
) {
  companion object {
    private var webviewId_acc = AtomicInteger(1)
  }

  var webViewList = ChangeableList<MultiViewItem>()

  init {
    webViewList.onChange {
      updateStateHook()
    }
    val wid = win.id
    /// 提供渲染适配
    windowAdapterManager.providers[wid] = @Composable { Render(it) }
    /// 窗口销毁的时候，移除适配器
    win.onDestroy {
      windowAdapterManager.providers.remove(wid)
    }
    /// ipc 断开的时候，强制关闭窗口
    ipc.onClose {
      win.close(force = true)
    }
  }

  fun isLastView(viewItem: MultiViewItem) = webViewList.lastOrNull() == viewItem
  fun isFistView(viewItem: MultiViewItem) = webViewList.firstOrNull() == viewItem
  val lastViewOrNull get() = webViewList.lastOrNull()
  fun getWebView(webviewId: String) = webViewList.find { it.webviewId == webviewId }

  data class MultiViewItem(
    override val webviewId: String,
    override val webView: DWebView,
    override val state: WebViewState,
    override val navigator: WebViewNavigator,
    override val coroutineScope: CoroutineScope,
    override var hidden: Boolean = false,
    val win: WindowController
  ) : ViewItem {
//    val nativeUiController by lazy {
//      webView.activity?.let {
//        NativeUiController(it)
//      }
//        ?: throw Exception("webview un attached to activity")
//    }
  }

  var downLoadObserver: DownLoadObserver? = null

  /**
   * 打开WebView
   */
  suspend fun openWebView(url: String) = appendWebViewAsItem(createDwebView(url))

  suspend fun createDwebView(url: String): DWebView = withContext(mainAsyncExceptionHandler) {
    val currentActivity = win.context;// App.appContext
    val dWebView = DWebView(
      currentActivity, remoteMM, DWebView.Options(
        url = url,
        /// 我们会完全控制页面将如何离开，所以这里兜底默认为留在页面
        onDetachedFromWindowStrategy = DWebView.Options.DetachedFromWindowStrategy.Ignore,
      )
    )
    dWebView
  }

  @Synchronized
  fun appendWebViewAsItem(dWebView: DWebView) = runBlockingCatching(mainAsyncExceptionHandler) {
    val webviewId = "#w${webviewId_acc.getAndAdd(1)}"
    val state = WebViewState(WebContent.Url(dWebView.url ?: ""))
    val coroutineScope = CoroutineScope(CoroutineName(webviewId))
    val navigator = WebViewNavigator(coroutineScope)
    MultiViewItem(
      webviewId = webviewId,
      webView = dWebView,
      state = state,
      coroutineScope = coroutineScope,
      navigator = navigator,
      win = win,
    ).also { viewItem ->
      webViewList.add(viewItem)
      dWebView.onCloseWindow {
        closeWebView(webviewId)
      }
      webViewOpenSignal.emit(webviewId)
    }
  }.getOrThrow()

  /**
   * 关闭WebView
   */
  suspend fun closeWebView(webViewId: String) =
    webViewList.find { it.webviewId == webViewId }?.let { viewItem ->
      webViewList.remove(viewItem)
      withContext(Dispatchers.Main) {
        viewItem.webView.destroy()
      }
      webViewCloseSignal.emit(webViewId)
      return true
    } ?: false

  /**
   * 移除所有列表
   */
  suspend fun destroyWebView(): Boolean {
    withContext(Dispatchers.Main) {
      webViewList.forEach { viewItem ->
        viewItem.webView.destroy()
      }
    }
    webViewList.clear()
    this.downLoadObserver?.close() // 移除下载状态监听
    return true
  }

  /**
   * 将指定WebView移动到顶部显示
   */
  suspend fun moveToTopWebView(webviewId: String): Boolean {
    val viewItem = webViewList.find { it.webviewId == webviewId } ?: return false
    webViewList.remove(viewItem)
    webViewList.add(viewItem)
    return true
  }

  private suspend fun updateStateHook() {
    val currentState = JSONObject()
    debugMultiWebView("updateStateHook =>", webViewList.size)
    webViewList.map {
      val viewItem = JSONObject()
      viewItem.put("webviewId", it.webviewId)
      viewItem.put("isActivated", it.hidden)
      viewItem.put("url", (it.state.content as WebContent.Url).url)
      currentState.put(it.webviewId, viewItem)
    }

    ipc.postMessage(IpcEvent.fromUtf8("state", currentState.toString()))
  }

  private val webViewCloseSignal = Signal<String>()
  private val webViewOpenSignal = Signal<String>()

  fun onWebViewClose(cb: Callback<String>) = webViewCloseSignal.listen(cb)
  fun onWebViewOpen(cb: Callback<String>) = webViewOpenSignal.listen(cb)


}