package org.dweb_browser.browser.desk

import androidx.compose.runtime.Stable
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebViewNavigator
import com.google.accompanist.web.WebViewState
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.platform.IPlatformViewController
import org.dweb_browser.helper.platform.PlatformViewController

actual fun IDesktopController.Companion.create(
  deskNMM: DeskNMM,
  desktopServer: HttpDwebServer,
  runningApps: ChangeableMap<MMID, RunningApp>
): IDesktopController = DesktopController(deskNMM, desktopServer, runningApps)

@Stable
class DesktopController(
  private val deskNMM: DeskNMM,
  private val desktopServer: HttpDwebServer,
  private val runningApps: ChangeableMap<MMID, RunningApp>
) : IDesktopController(deskNMM, desktopServer, runningApps) {

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

  /**
   * 窗口管理器
   */
  override val desktopWindowsManager
    get() = DesktopWindowsManager.getOrPutInstance(PlatformViewController(this.activity!!)) { dwm ->

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
        detachedStrategy = DWebViewOptions.DetachedStrategy.Ignore,
      )
    );
    // 隐藏滚动条
    webView.isVerticalScrollBarEnabled = false
    val state = WebViewState(WebContent.Url(initUrl))
    val coroutineScope = CoroutineScope(CoroutineName("desk/main-dwebview/$name"))
    val navigator = WebViewNavigator(coroutineScope)
    MainDwebView(name, webView, state, navigator)
  }
}
