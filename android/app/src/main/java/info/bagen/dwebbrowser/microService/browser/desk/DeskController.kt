package info.bagen.dwebbrowser.microService.browser.desk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import info.bagen.dwebbrowser.App
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.PromiseOut
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

  /**
   * 窗口管理器
   */
  val desktopWindowsManager get() = DesktopWindowsManager.getInstance(this.activity!!)

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

  fun createMainDwebView() = DWebView(
    activity ?: App.appContext, desktopNMM, DWebView.Options(
      url = "",
      onDetachedFromWindowStrategy = DWebView.Options.DetachedFromWindowStrategy.Ignore,
    )
  )

  fun getDesktopUrl() = desktopServer.startResult.urlInfo.buildInternalUrl().let {
    it.path("/desktop.html")
      .query("api-base", desktopServer.startResult.urlInfo.buildPublicUrl().toString())
  }
}
