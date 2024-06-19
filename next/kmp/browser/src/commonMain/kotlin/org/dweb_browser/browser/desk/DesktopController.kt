package org.dweb_browser.browser.desk

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeContent
import androidx.compose.material.Text
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
import io.ktor.http.Url
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.browser.common.createDwebView
import org.dweb_browser.browser.desk.types.DeskAppMetaData
import org.dweb_browser.browser.desk.upgrade.NewVersionController
import org.dweb_browser.browser.desk.upgrade.NewVersionItem
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.helper.Bounds
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.ENV_SWITCH_KEY
import org.dweb_browser.helper.OffListener
import org.dweb_browser.helper.SafeHashSet
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.build
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.envSwitch
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.from
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.render.NativeBackHandler

open class DesktopAppController constructor(open val deskNMM: DeskNMM.DeskRuntime) {

  suspend fun open(mmid: String) {
    deskNMM.nativeFetch("file://desk.browser.dweb/openAppOrActivate?app_id=$mmid")
  }

  suspend fun quit(mmid: String) {
    deskNMM.nativeFetch("file://desk.browser.dweb/closeApp?app_id=$mmid")
  }

  suspend fun search(words: String) {
    deskNMM.nativeFetch("file://web.browser.dweb/search?q=$words")
  }
}


@Stable
open class DesktopController private constructor(
  override val deskNMM: DeskNMM.DeskRuntime,
  private val desktopServer: HttpDwebServer,
  private val runningApps: ChangeableMap<MMID, RunningApp>,
): DesktopAppController(deskNMM) {
  val newVersionController = NewVersionController(deskNMM, this)

  @OptIn(ExperimentalCoroutinesApi::class)
  var activity: IPureViewController? = null
    set(value) {
      if (field == value) {
        return
      }
      field = value
      if (_desktopView.isCompleted) {
        val oldView = _desktopView.getCompleted()
        oldView.lifecycleScope.launch {
          oldView.destroy()
        }
        _desktopView = CompletableDeferred()
      }
      if (value != null) {
        deskNMM.scopeLaunch(cancelable = true) {
          runCatching {
            _desktopView.complete(createDesktopView(value))
          }.onFailure {
            _desktopView.completeExceptionally(it)
          }
        }
      }
    }
  private var _desktopView = CompletableDeferred<IDWebView>()
  private suspend fun createDesktopView(activity: IPureViewController): IDWebView {
    val options = DWebViewOptions(
      url = getDesktopUrl().toString(),
      privateNet = true,
      openDevTools = envSwitch.isEnabled(ENV_SWITCH_KEY.DESKTOP_DEVTOOLS),
      detachedStrategy = DWebViewOptions.DetachedStrategy.Ignore,
      displayCutoutStrategy = DWebViewOptions.DisplayCutoutStrategy.Default,
      viewId = 1,
      subDataDirName = "desktop"
    );

    val webView = activity.createDwebView(deskNMM, options)
    // 隐藏滚动条
    webView.setVerticalScrollBarVisible(false)
    webView.setHorizontalScrollBarVisible(false)

    deskNMM.onBeforeShutdown {
      deskNMM.scopeLaunch(cancelable = false) {
        webView.destroy()
      }
    }
    return webView
  }

  private suspend fun desktopView() = _desktopView.await()

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
        view.lifecycleScope.launch {
          view.goBack()
        }
      }
    }
  }

  companion object {
    suspend fun create(
      deskNMM: DeskNMM.DeskRuntime,
      desktopServer: HttpDwebServer,
      runningApps: ChangeableMap<MMID, RunningApp>,
    ) = DesktopController(deskNMM, desktopServer, runningApps)
  }

  // 状态更新信号
  internal val updateFlow = MutableSharedFlow<String>()
  val onUpdate = channelFlow {
    val reasons = SafeHashSet<String>()
    updateFlow.onEach {
      reasons.addAll(it.split("|"))
    }.conflate().collect {
      delay(100)
      val result = reasons.sync {
        val result = joinToString("|")
        clear()
        result
      }
      if (result.isNotEmpty()) {
        send(result)
      }
    }
    close()
  }.shareIn(deskNMM.getRuntimeScope(), started = SharingStarted.Eagerly)

  init {
    runningApps.onChange { map ->
      updateFlow.emit("apps")
    }
  }

  suspend fun detail(mmid: String) {
    deskNMM.nativeFetch("file://jmm.browser.dweb/detail?app_id=$mmid")
  }

  suspend fun uninstall(mmid: String) {
    deskNMM.nativeFetch("file://jmm.browser.dweb/uninstall?app_id=$mmid")
  }

  suspend fun share(mmid: String) {
    // TODO: 分享
  }

  suspend fun isSystermApp(mmid: String) = !deskNMM.nativeFetch("file://jmm.browser.dweb/isInstalled?app_id=$mmid").boolean()

  private val appSortList = DaskSortStore(deskNMM)
  suspend fun getDesktopApps(): List<DeskAppMetaData> {
    val apps =
      deskNMM.bootstrapContext.dns.search(MICRO_MODULE_CATEGORY.Application).toMutableList()
    // 简单的排序再渲染
    val sortList = appSortList.getApps()
    apps.sortBy { sortList.indexOf(it.mmid) }
    val runApps = apps.map { metaData ->
      return@map DeskAppMetaData().apply {
        running = runningApps.containsKey(metaData.mmid)
        winStates = getDesktopWindowsManager().getWindowStates(metaData.mmid)
        //...复制metaData属性
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
      val watchWindows = mutableMapOf<WindowController, OffListener<*>>()
      fun watchAllWindows() {
        watchWindows.keys.subtract(dwm.allWindows).forEach { win ->
          watchWindows.remove(win)?.invoke()
        }
        for (win in dwm.allWindows) {
          if (watchWindows.contains(win)) {
            continue
          }
          watchWindows[win] = win.state.observable.onChange {
            updateFlow.emit(it.key.fieldName)
          }
        }
      }

      /// 但有窗口信号变动的时候，确保 MicroModule.IpcEvent<Activity> 事件被激活
      dwm.allWindowsFlow.collectIn(dwm.viewController.lifecycleScope) {
        watchAllWindows()
        updateFlow.emit("windows")
        _activitySignal.emit()
      }
      watchAllWindows()

      preDesktopWindowsManager?.also { preDwm ->
        deskNMM.scopeLaunch(Dispatchers.Main, cancelable = true) {
          /// 窗口迁移
          preDwm.moveWindowsTo(dwm)
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


  private fun getDesktopUrl() = when (val url = envSwitch.get(ENV_SWITCH_KEY.DESKTOP_DEV_URL)) {
    "" -> desktopServer.startResult.urlInfo.buildInternalUrl().build {
      resolvePath("/desktop.html")
    }

    else -> Url(url)
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

/**
 * 获取当前版本，存储的版本，以及在线加载最新版本
 */
expect suspend fun loadApplicationNewVersion(): NewVersionItem?