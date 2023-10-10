package info.bagen.dwebbrowser.microService.browser.desk

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebViewNavigator
import com.google.accompanist.web.WebViewState
import info.bagen.dwebbrowser.microService.browser.desk.types.DeskAppMetaData
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.build
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.std.http.HttpDwebServer

@Stable
class DesktopController(
  private val deskNMM: DeskNMM,
  private val desktopServer: HttpDwebServer,
  private val runningApps: ChangeableMap<MMID, RunningApp>
) {

  internal val updateSignal = SimpleSignal()
  val onUpdate = updateSignal.toListener()

  init {
    runningApps.onChange {
      updateSignal.emit()
    }
  }

  suspend fun getDesktopApps(): List<DeskAppMetaData> {
    val apps = deskNMM.bootstrapContext.dns.search(MICRO_MODULE_CATEGORY.Application)
    val runApps = apps.map { metaData ->
      return@map DeskAppMetaData().apply {
        running = runningApps.containsKey(metaData.mmid)
        winStates = desktopWindowsManager.getWindowStates(metaData.mmid)
        winStates.firstOrNull()?.let { state ->
          debugDesk(
            "getDesktopApps", "winStates -> ${winStates.size}, ${state.mode}, ${state.focus}"
          )
        }
        assign(metaData.manifest)
      }
    }
    return runApps
  }

  private var activityTask = PromiseOut<DesktopActivity>()

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
    get() = DesktopWindowsManager.getOrPutInstance(this.activity!!) { dwm ->

      dwm.hasMaximizedWins.onChange { updateSignal.emit() }

      /// 但有窗口信号变动的时候，确保 MicroModule.IpcEvent<Activity> 事件被激活
      dwm.allWindows.onChange {
        updateSignal.emit()
        _activitySignal.emit()
      }.removeWhen(dwm.viewController.lifecycleScope)

      preDesktopWindowsManager?.also { preDwm ->
        dwm.viewController.lifecycleScope.launch {
          /// 窗口迁移
          preDwm.moveWindows(dwm)
          preDesktopWindowsManager = null
        }
      }
      preDesktopWindowsManager = dwm
    }

  data class MainDwebView(
    val name: String,
    val webView: DWebViewEngine,
    val state: WebViewState,
    val navigator: WebViewNavigator
  )

  private val mainDwebViews = mutableMapOf<String, MainDwebView>()

  fun createMainDwebView(name: String, initUrl: String = "") = mainDwebViews.getOrPut(name) {
    val webView = DWebViewEngine(
      activity ?: deskNMM.getAppContext(), deskNMM, DWebViewOptions(
        url = initUrl,
        onDetachedFromWindowStrategy = DWebViewOptions.DetachedFromWindowStrategy.Ignore,
      )
    );
    val state = WebViewState(WebContent.Url(initUrl))
    val coroutineScope = CoroutineScope(CoroutineName("desk/main-dwebview/$name"))
    val navigator = WebViewNavigator(coroutineScope)
    MainDwebView(name, webView, state, navigator)
  }

  fun getDesktopUrl() = desktopServer.startResult.urlInfo.buildInternalUrl().build {
    resolvePath("/desktop.html")
    parameters["api-base"] = desktopServer.startResult.urlInfo.buildPublicUrl().toString()
  }


  private val _activitySignal = SimpleSignal()
  val onActivity = _activitySignal.toListener()


  data class AlertMessage(val title: String, val message: String)

  internal val alertMessages = mutableStateListOf<AlertMessage>()
  fun showAlert(reason: Throwable) {
    val title = reason.cause?.message ?: "异常"
    val message = reason.message ?: "未知原因"
    alertMessages.add(AlertMessage(title, message))
  }
}
