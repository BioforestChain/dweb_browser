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
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.MMID
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.sys.http.HttpDwebServer
import org.http4k.core.query

@Stable
class DeskController(
  private val desktopNMM: DesktopNMM,
  private val desktopServer: HttpDwebServer,
  private val runningApps: ChangeableMap<MMID, Ipc>
) {

  internal val updateSignal = SimpleSignal()
  val onUpdate = updateSignal.toListener()

  init {
    runningApps.onChange {
      updateSignal.emit()
    }
    desktopNMM.bootstrapContext.dns.onChange {
      updateSignal.emit()
    }
  }

  fun getDesktopApps(): List<DeskAppMetaData> {
    var runApps = listOf<DeskAppMetaData>()
    runBlockingCatching(ioAsyncExceptionHandler) {
      val apps = desktopNMM.bootstrapContext.dns.search(MICRO_MODULE_CATEGORY.Application)
      runApps = apps.map { metaData ->
        return@map DeskAppMetaData(
          running = runningApps.containsKey(metaData.mmid),
        ).setMetaData(metaData)
      }
    }.getOrThrow()
    return runApps
  }

  fun getInstallApps() = getDesktopApps().toMutableList()

  fun getOpenApps() = getDesktopApps().filter { it.running }.toMutableList()

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
  val floatViewState: MutableState<Boolean> = mutableStateOf(true)

  private var preDesktopWindowsManager: DesktopWindowsManager? = null

  /**
   * 窗口管理器
   */
  val desktopWindowsManager
    get() = DesktopWindowsManager.getInstance(this.activity!!) { dwm ->
      /// 但有窗口信号变动的时候，确保 Activity 事件被激活
      dwm.allWindows.onChange {
        _activitySignal.emit()
      }.also { off ->
        dwm.activity.onDestroyActivity {
          off()
        }
      }

      preDesktopWindowsManager?.also { preDwm ->
        /// 窗口迁移
        for (win in preDwm.allWindows.keys.toSet()/*拷贝一份避免并发修改导致的问题*/) {
          preDwm.removeWindow(win)
          dwm.addNewWindow(win)
        }
        preDesktopWindowsManager = null
      }
      preDesktopWindowsManager = dwm
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

  data class MainDwebView(
    val name: String,
    val webView: DWebView,
    val state: WebViewState,
    val navigator: WebViewNavigator
  )

  val mainDwebViews = mutableMapOf<String, MainDwebView>()

  fun createMainDwebView(name: String, initUrl: String = "") = mainDwebViews.getOrPut(name) {
    val webView = DWebView(
      activity ?: App.appContext, desktopNMM, DWebView.Options(
        url = initUrl,
        onDetachedFromWindowStrategy = DWebView.Options.DetachedFromWindowStrategy.Ignore,
      )
    );
    val state = WebViewState(WebContent.Url(initUrl))
    val coroutineScope = CoroutineScope(CoroutineName("desk/main-dwebview/$name"))
    val navigator = WebViewNavigator(coroutineScope)
    MainDwebView(name, webView, state, navigator)
  }

  fun getDesktopUrl() = desktopServer.startResult.urlInfo.buildInternalUrl()
    .path("/desktop.html")
    .query("api-base", desktopServer.startResult.urlInfo.buildPublicUrl().toString())


  private val _activitySignal = SimpleSignal()
  val onActivity = _activitySignal.toListener()
}
