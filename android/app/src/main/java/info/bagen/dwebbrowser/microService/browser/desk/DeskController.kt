package info.bagen.dwebbrowser.microService.browser.desk

import androidx.compose.runtime.Stable
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebViewNavigator
import com.google.accompanist.web.WebViewState
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.browser.desk.types.DeskAppMetaData
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.helper.with
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.types.MMID
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
    val apps = runBlockingCatching(ioAsyncExceptionHandler) {
      desktopNMM.bootstrapContext.dns.search(MICRO_MODULE_CATEGORY.Application)
    }.getOrThrow()
    val runApps = apps.map { metaData ->
      return@map DeskAppMetaData().with {
        running = runningApps.containsKey(metaData.mmid)
        assign(metaData.manifest)
      }
    }
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

  private var preDesktopWindowsManager: DesktopWindowsManager? = null

  /**
   * 窗口管理器
   */
  val desktopWindowsManager
    get() = DesktopWindowsManager.getInstance(this.activity!!) { dwm ->
      /// 但有窗口信号变动的时候，确保 MicroModule.IpcEvent<Activity> 事件被激活
      dwm.allWindows.onChange {
        _activitySignal.emit()
      }.removeWhen(dwm.activity.onDestroyActivity)

      preDesktopWindowsManager?.also { preDwm ->
        dwm.activity.lifecycleScope.launch {
          /// 窗口迁移
          preDwm.moveWindows(dwm)
          preDesktopWindowsManager = null
        }
      }
      preDesktopWindowsManager = dwm
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

  fun getDesktopUrl() = desktopServer.startResult.urlInfo.buildInternalUrl().path("/desktop.html")
    .query("api-base", desktopServer.startResult.urlInfo.buildPublicUrl().toString())


  private val _activitySignal = SimpleSignal()
  val onActivity = _activitySignal.toListener()
}
