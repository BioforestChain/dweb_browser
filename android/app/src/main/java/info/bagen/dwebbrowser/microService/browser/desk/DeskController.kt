package info.bagen.dwebbrowser.microService.browser.desk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebViewNavigator
import com.google.accompanist.web.WebViewState
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.browser.jmm.EIpcEvent
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.dwebview.base.DWebViewItem
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.microservice.ipc.helper.IpcEvent
import org.dweb_browser.microservice.sys.http.HttpDwebServer
import org.http4k.core.query
import java.util.concurrent.atomic.AtomicInteger

@Stable
class DeskController(
  private val desktopNMM: DesktopNMM,
  private val taskbarServer: HttpDwebServer,
  private val desktopServer: HttpDwebServer
) {

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
  fun effect(activity: DesktopActivity): DeskController {
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

  fun createMainDwebView() = DWebView(
    activity ?: App.appContext, desktopNMM,
    DWebView.Options(
      url = "",
      /// 我们会完全控制页面将如何离开，所以这里兜底默认为留在页面
      onDetachedFromWindowStrategy = DWebView.Options.DetachedFromWindowStrategy.Ignore,
    )
  )

//  @Synchronized
//  private fun appendWebViewAsItem(dWebView: DWebView, url: String): DWebViewItem {
//    val webviewId = "#w${webviewId_acc.getAndAdd(1)}"
//    val state = WebViewState(WebContent.Url(url))
//    val coroutineScope = CoroutineScope(CoroutineName(webviewId))
//    val navigator = WebViewNavigator(coroutineScope)
//    val viewItem = DWebViewItem(
//      webviewId = webviewId,
//      webView = dWebView,
//      state = state,
//      coroutineScope = coroutineScope,
//      navigator = navigator,
//    )
//    return viewItem
//  }

  fun getDesktopUrl() = desktopServer.startResult.urlInfo.buildInternalUrl().let {
    it.path("/desktop.html")
      .query("api-base", desktopServer.startResult.urlInfo.buildPublicUrl().toString())
  }
}
