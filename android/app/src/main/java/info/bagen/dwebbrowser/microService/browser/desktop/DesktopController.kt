package info.bagen.dwebbrowser.microService.browser.desktop

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebViewNavigator
import com.google.accompanist.web.WebViewState
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.browser.jmm.EIpcEvent
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.browserUI.ui.browser.ConstUrl
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.dwebview.base.DWebViewItem
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.ipc.helper.IpcEvent
import org.dweb_browser.microservice.sys.dns.nativeFetch
import org.dweb_browser.microservice.sys.http.CORS_HEADERS
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.query
import org.http4k.lens.Header
import java.util.concurrent.atomic.AtomicInteger

@Stable
class DesktopController(private val desktopNMM: DesktopNMM) {

  companion object {
    private var webviewId_acc = AtomicInteger(1)
  }

  fun getInstallApps() = desktopNMM.getDesktopApps().toMutableList()

  fun getOpenApps() = desktopNMM.getDesktopApps().filter { it.running }.toMutableList()

  private var activityTask = PromiseOut<DesktopActivity>()
  suspend fun waitActivityCreated() = activityTask.waitPromise()

  var activity: DesktopActivity? = null
    set(value) {
      if (field == value) {
        return
      }
      field = value
      if (value == null) {
        activityTask = PromiseOut()
      } else {
        activityTask.resolve(value)
      }
    }

  val currentInsets: MutableState<WindowInsetsCompat> by lazy {
    mutableStateOf(
      WindowInsetsCompat.toWindowInsetsCompat(
        activity!!.window.decorView.rootWindowInsets
      )
    )
  }

  @Composable
  fun effect(activity: DesktopActivity): DesktopController {
    /**
     * 这个 NativeUI 的逻辑是工作在全屏幕下，所以会使得默认覆盖 系统 UI
     */
    SideEffect {
      WindowCompat.setDecorFitsSystemWindows(activity.window, false)
      /// system-bar 一旦隐藏（visible = false），那么被手势划出来后，过一会儿自动回去
      //windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

      ViewCompat.setOnApplyWindowInsetsListener(activity.window.decorView) { _, insets ->
        currentInsets.value = insets
        insets
      }

    }
    return this
  }

  /**
   * 对Taskbar自身进行resize
   * 根据web元素的大小进行自适应调整
   *
   * @returns 如果视图发生了真实的改变（不论是否变成说要的结果），则返回 true
   */
  fun resize(width: Number, height: Number) {

  }

  /**
   * 将其它视图临时最小化到 TaskbarView/TooggleDesktopButton 按钮里头，在此点击该按钮可以释放这些临时视图到原本的状态
   */
  fun toggleDesktopView() {

  }

  private val openLock = Mutex()
  suspend fun openApp(deskAppMetaData: DeskAppMetaData) {
    openLock.withLock {
      val (ipc) = desktopNMM.bootstrapContext.dns.connect(deskAppMetaData.mmid)
      debugDesktop("openApp", "postMessage==>activity ${ipc.remote.mmid}")
      ipc.postMessage(IpcEvent.fromUtf8(EIpcEvent.Activity.event, ""))
    }
  }

  fun createMainDwebView(url: String = ConstUrl.NEW_TAB.url): DWebViewItem {
    val context = activity ?: App.appContext
    val dWebView = DWebView(
      context, desktopNMM, desktopNMM, DWebView.Options(
        url = url,
        /// 我们会完全控制页面将如何离开，所以这里兜底默认为留在页面
        onDetachedFromWindowStrategy = DWebView.Options.DetachedFromWindowStrategy.Ignore,
      ), activity
    ).also {
      it.webViewClient = DesktopWebViewClient(desktopNMM)
    }
    return appendWebViewAsItem(dWebView, url)
  }

  @Synchronized
  private fun appendWebViewAsItem(dWebView: DWebView, url: String): DWebViewItem {
    val webviewId = "#w${webviewId_acc.getAndAdd(1)}"
    val state = WebViewState(WebContent.Url(url))
    val coroutineScope = CoroutineScope(CoroutineName(webviewId))
    val navigator = WebViewNavigator(coroutineScope)
    val viewItem = DWebViewItem(
      webviewId = webviewId,
      webView = dWebView,
      state = state,
      coroutineScope = coroutineScope,
      navigator = navigator,
    )
    viewItem.webView.webViewClient = DesktopWebViewClient(desktopNMM)
    return viewItem
  }
}

class DesktopWebViewClient(private val microModule: MicroModule) : AccompanistWebViewClient() {

  override fun shouldInterceptRequest(
    view: WebView, request: WebResourceRequest
  ): WebResourceResponse? {
    var response: Response? = null
    val url = request.url
    if (url.scheme == "http" && url.host == "localhost") {
      response = runBlockingCatching(ioAsyncExceptionHandler) {
        val urlPathSegments = url.pathSegments.filter { it.isNotEmpty() }
        // readAccept
        if (urlPathSegments.toString().contains("readAccept")) {
          return@runBlockingCatching Response(Status.OK).body("""{"accept":"${request.requestHeaders["Accept"]}"}""")
        }
        debugDesktop("shouldInterceptRequest", url)
        return@runBlockingCatching if (urlPathSegments.getOrNull(1) == "api") {
          val pathSegments = urlPathSegments.drop(1)
          // API
          microModule.nativeFetch(
            "file://${
              pathSegments.drop(1).joinToString("/")
            }?${request.url.query}"
          )
        } else {
          microModule.nativeFetch(
            "file:///sys/browser/${
              if (urlPathSegments.isEmpty()) "desktop.html" else urlPathSegments.joinToString("/")
            }"
          )
        }
      }.getOrThrow()
    } else if (request.url.scheme == "dweb") { // 负责拦截browser的dweb_deeplink
      runBlockingCatching(ioAsyncExceptionHandler) {
        microModule.nativeFetch(request.url.toString())
      }.getOrThrow()
      response = Response(
        Status.OK
      )
    } else if (request.url.path?.contains("metadata.json") == true) { // 如果地址结尾是 metadata.json 目前是作为安装地址，跳转到安装界面
      response = runBlockingCatching(ioAsyncExceptionHandler) {
        microModule.nativeFetch(
          org.http4k.core.Uri.of("file://jmm.browser.dweb/install?")
            .query("url", request.url.toString())
        )
      }.getOrThrow()
    }
    if (response !== null) {
      val contentType = Header.CONTENT_TYPE(response)
      return WebResourceResponse(
        contentType?.value,
        contentType?.directives?.find { it.first == "charset" }?.second,
        response.status.code,
        response.status.description,
        CORS_HEADERS.toMap(),
        response.body.stream,
      )
    }
    return super.shouldInterceptRequest(view, request)
  }
}