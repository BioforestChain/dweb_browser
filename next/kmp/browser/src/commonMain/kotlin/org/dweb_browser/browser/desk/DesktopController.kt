package org.dweb_browser.browser.desk

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.browser.common.createDwebView
import org.dweb_browser.browser.desk.types.DeskAppMetaData
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.helper.Bounds
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.build
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.from
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.sys.window.render.NativeBackHandler

@Stable
open class DesktopController private constructor(
  private val deskNMM: DeskNMM,
  private val desktopServer: HttpDwebServer,
  private val runningApps: ChangeableMap<MMID, RunningApp>,
) {
  var activity: IPureViewController? = null
    set(value) {
      if (field == value) {
        return
      }
      field = value

      if (_desktopView.isResolved) {
        _desktopView = PromiseOut()
      }
      if (value != null) {
        _desktopView.resolve(
          deskNMM.ioAsyncScope.async { createDesktopView(value) },
          deskNMM.ioAsyncScope
        )
      }
    }
  private var _desktopView = PromiseOut<IDWebView>()
  private suspend fun createDesktopView(activity: IPureViewController): IDWebView {
    val options = DWebViewOptions(
      url = getDesktopUrl().toString(),
      privateNet = true,
      detachedStrategy = DWebViewOptions.DetachedStrategy.Ignore,
      displayCutoutStrategy = DWebViewOptions.DisplayCutoutStrategy.Default,
    );
    val webView = activity.createDwebView(deskNMM, options)
    // 隐藏滚动条
    webView.setVerticalScrollBarVisible(false)
    return webView
  }

  private suspend fun desktopView() = _desktopView.waitPromise()

  @Composable
  fun DesktopView(content: @Composable IDWebView.() -> Unit) {
    var view by remember { mutableStateOf<IDWebView?>(null) }
    LaunchedEffect(this) {
      view = desktopView()
    }
    view?.also { view ->
      val safeContent = WindowInsets.safeContent
      val density = LocalDensity.current
      val layoutDirection = LocalLayoutDirection.current
      LaunchedEffect(safeContent, density, layoutDirection) {
        view.setSafeAreaInset(
          Bounds(
            left = safeContent.getLeft(density, layoutDirection) / density.density,
            top = safeContent.getTop(density) / density.density,
            right = safeContent.getRight(density, layoutDirection) / density.density,
            bottom = safeContent.getBottom(density) / density.density,
          )
        )
      }
      view.content()
      val canGoBack by view.canGoBackStateFlow.collectAsState()
      NativeBackHandler(canGoBack) {
        view.ioScope.launch {
          view.goBack()
        }
      }
    }
  }

  companion object {
    suspend fun create(
      deskNMM: DeskNMM,
      desktopServer: HttpDwebServer,
      runningApps: ChangeableMap<MMID, RunningApp>
    ) = DesktopController(deskNMM, desktopServer, runningApps)
  }

  // 状态更新信号
  internal val updateSignal = SimpleSignal()
  val onUpdate = updateSignal.toListener()

  init {
    runningApps.onChange { map ->
      updateSignal.emit()
    }
  }

  private val appSortList = DaskSortStore(deskNMM)
  suspend fun getDesktopApps(): List<DeskAppMetaData> {
    val apps = deskNMM.bootstrapContext.dns.search(MICRO_MODULE_CATEGORY.Application)
    // 简单的排序再渲染
    val sortList = appSortList.getApps()
    apps.sortBy { sortList.indexOf(it.mmid) }
    val runApps = apps.map { metaData ->
      return@map DeskAppMetaData().apply {
        running = runningApps.containsKey(metaData.mmid)
        winStates = getDesktopWindowsManager().getWindowStates(metaData.mmid)
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

  private var preDesktopWindowsManager: DesktopWindowsManager? = null

  private val wmLock = Mutex()

  /**
   * 窗口管理器
   */
  suspend fun getDesktopWindowsManager() = wmLock.withLock {
    val vc = this.activity!!
    DesktopWindowsManager.getOrPutInstance(vc, IPureViewBox.from(vc)) { dwm ->
      dwm.hasMaximizedWins.onChange { updateSignal.emit() }

      /// 但有窗口信号变动的时候，确保 MicroModule.IpcEvent<Activity> 事件被激活
      dwm.allWindows.onChange {
        updateSignal.emit()
        _activitySignal.emit()
      }.removeWhen(dwm.viewController.lifecycleScope)

      preDesktopWindowsManager?.also { preDwm ->
        deskNMM.ioAsyncScope.launch(Dispatchers.Main) {
          /// 窗口迁移
          preDwm.moveWindows(dwm)
        }
      }
      preDesktopWindowsManager = dwm
    }
  }

  @Composable
  fun DesktopWindowsManager(content: @Composable DesktopWindowsManager.() -> Unit) {
    var windowsManager by remember { mutableStateOf<DesktopWindowsManager?>(null) }
    LaunchedEffect(Unit) {
      windowsManager = getDesktopWindowsManager()
    }
    windowsManager?.content()
  }


  private fun getDesktopUrl() = desktopServer.startResult.urlInfo.buildInternalUrl().build {
    resolvePath("/desktop.html")
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
