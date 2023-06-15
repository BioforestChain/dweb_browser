package info.bagen.dwebbrowser.microService.browser.mwebview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebViewNavigator
import com.google.accompanist.web.WebViewState
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.browser.nativeui.NativeUiController
import info.bagen.dwebbrowser.microService.browser.webview.DWebView
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dweb_browser.helper.Callback
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.help.Mmid
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.message.IpcEvent
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

/**
 * MWebView 是为其它模块提供 GUI 的程序模块，所以这里需要传入两个模块：localeMM 与 remoteMM
 * remoteMM 只是一层抽象，未来如何需要可以通过网络成实现
 */
@Stable
class MultiWebViewController(
  val mmid: Mmid,
  val localeMM: MultiWebViewNMM,
  val remoteMM: MicroModule,
) {
  companion object {
    private var webviewId_acc = AtomicInteger(1)
  }

  private var webViewList = mutableStateListOf<ViewItem>()

  //    @Composable
//    fun effectItem(viewItem:ViewItem) {
//        debugMultiWebView("effectItem","${viewItem.webviewId} ${viewItem.hidden}")
//        LaunchedEffect(viewItem) {
//               snapshotFlow { viewItem.hidden }.collect {
//                   updateStateHook()
//               }
//           }
//    }
  @Composable
  fun effect() {
    LaunchedEffect(webViewList) {
      snapshotFlow { webViewList.size }.collect {
        updateStateHook()
      }
    }
  }

  @Composable
  fun eachView(action: @Composable (viewItem: ViewItem) -> Unit) {
    webViewList.forEachIndexed { _, viewItem ->
      action(viewItem)
    }
  }

  fun isLastView(viewItem: ViewItem) = webViewList.lastOrNull() == viewItem
  fun isFistView(viewItem: ViewItem) = webViewList.firstOrNull() == viewItem
  val lastViewOrNull get() = webViewList.lastOrNull()

  private val mIpcMap = mutableMapOf<Mmid, Ipc>()

  data class ViewItem(
    val webviewId: String,
    val webView: DWebView,
    val state: WebViewState,
    val navigator: WebViewNavigator,
    val coroutineScope: CoroutineScope,
    var hidden: MutableState<Boolean> = mutableStateOf(false)
  ) {
    val nativeUiController by lazy {

      webView.activity?.let { NativeUiController(it) }
        ?: throw Exception("webview un attached to activity")
    }
  }

  private var activityTask = PromiseOut<MultiWebViewActivity>()
  suspend fun waitActivityCreated() = activityTask.waitPromise()

  var activity: MultiWebViewActivity? = null
    set(value) {
      if (field == value) {
        return
      }
      field = value
      for (webview in webViewList) {
        webview.webView.activity = value
      }
      if (value == null) {
        activityTask = PromiseOut()
      } else {
        activityTask.resolve(value)
      }
    }

  /**
   * 打开WebView
   */
  suspend fun openWebView(url: String) = appendWebViewAsItem(createDwebView(url))

  suspend fun createDwebView(url: String): DWebView = withContext(mainAsyncExceptionHandler) {
    val currentActivity = activity ?: App.appContext
    val dWebView = DWebView(
      currentActivity, localeMM, remoteMM, DWebView.Options(
        url = url,
        /// 我们会完全控制页面将如何离开，所以这里兜底默认为留在页面
        onDetachedFromWindowStrategy = DWebView.Options.DetachedFromWindowStrategy.Ignore,
      ), activity
    )
    dWebView
  }

  @Synchronized
  fun appendWebViewAsItem(dWebView: DWebView) = runBlockingCatching(
    Dispatchers.Main
  ) {
    val webviewId = "#w${webviewId_acc.getAndAdd(1)}"
    val state = WebViewState(WebContent.Url(dWebView.url ?: ""))
    val coroutineScope = CoroutineScope(CoroutineName(webviewId))
    val navigator = WebViewNavigator(coroutineScope)
    ViewItem(
      webviewId = webviewId,
      webView = dWebView,
      state = state,
      coroutineScope = coroutineScope,
      navigator = navigator,
    ).also { viewItem ->
      webViewList.add(viewItem)
      dWebView.onCloseWindow {
        closeWebView(webviewId)
      }
      viewItem.coroutineScope.launch {
        webViewOpenSignal.emit(webviewId)
      }
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
    webViewList.forEach { viewItem ->
      withContext(Dispatchers.Main) {
        viewItem.webView.destroy()
      }
    }
    webViewList.clear()
    this.activity?.finish()
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
      viewItem.put("isActivated", it.hidden.value)
      viewItem.put("url", (it.state.content as WebContent.Url).url)
      currentState.put(it.webviewId, viewItem)
    }
    mIpcMap.getOrPut(mmid) {
      val (ipc) = localeMM.connect(mmid)
      ipc.onEvent {
        debugMultiWebView("event", "name=${it.event.name},data=${it.event.data}")
      }
      ipc
    }.also { ipc ->
      ipc.postMessage(IpcEvent.fromUtf8("state", currentState.toString()))
    }
  }

  private val webViewCloseSignal = Signal<String>()
  private val webViewOpenSignal = Signal<String>()

  fun onWebViewClose(cb: Callback<String>) = webViewCloseSignal.listen(cb)
  fun onWebViewOpen(cb: Callback<String>) = webViewOpenSignal.listen(cb)

}
