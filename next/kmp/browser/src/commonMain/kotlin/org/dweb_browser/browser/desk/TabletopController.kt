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
import io.ktor.http.Url
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.browser.common.createDwebView
import org.dweb_browser.browser.desk.model.TabletopAppModel
import org.dweb_browser.browser.desk.types.DeskAppMetaData
import org.dweb_browser.browser.web.WebLinkMicroModule
import org.dweb_browser.browser.web.data.WebLinkManifest
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.OffListener
import org.dweb_browser.helper.PureBounds
import org.dweb_browser.helper.SafeHashSet
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.build
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.NativeBackHandler
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.from
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.sys.window.core.WindowController

@Stable
open class TabletopController private constructor(
  final val deskNMM: DeskNMM.DeskRuntime,
  private val tabletopServer: HttpDwebServer,
  private val runningApps: ChangeableMap<MMID, RunningApp>,
) {
  private val openingApps = mutableSetOf<MMID>()
  internal val appsFlow = MutableStateFlow(emptyList<TabletopAppModel>())
  private suspend fun upsetApps() {
    appsFlow.value = getTabletopApps().map { appMetaData ->
      val runStatus = if (appMetaData.running) {
        openingApps.remove(appMetaData.mmid)
        TabletopAppModel.DesktopAppRunStatus.Opened
      } else if (openingApps.contains(appMetaData.mmid)) {
        TabletopAppModel.DesktopAppRunStatus.Opening
      } else {
        TabletopAppModel.DesktopAppRunStatus.Close
      }

      appsFlow.value.find { oldApp ->
        oldApp.mmid == appMetaData.mmid
      }?.also { it.running = runStatus } ?: TabletopAppModel(
        appMetaData = appMetaData, initRunningState = runStatus
      )
    }
  }

  suspend fun open(mmid: MMID) {
    val app = appsFlow.value.find { it.mmid == mmid } ?: return
    when (val webLink = app.webLink) {
      null -> {
        if (app.running == TabletopAppModel.DesktopAppRunStatus.Close) {
          app.running = TabletopAppModel.DesktopAppRunStatus.Opening
          openingApps.add(mmid)
        }
        // 不论如何都进行 open，因为这本质是 openAppOrActivate
        deskNMM.open(mmid)
      }

      else -> {
        deskNMM.nativeFetch(webLink)
//        deskNMM.connect(app.mmid).request(PureClientRequest(webLink, PureMethod.GET))
      }
    }
  }

  suspend fun quit(mmid: MMID) {
    openingApps.remove(mmid)
    deskNMM.quit(mmid)
  }

  // val newVersionController = NewVersionController(deskNMM, this)
  // 针对 WebLink 的管理部分 begin
  private val webLinkStore = WebLinkStore(deskNMM)
  suspend fun loadWebLinks() {
    webLinkStore.getAll().map { (_, webLinkManifest) ->
      deskNMM.bootstrapContext.dns.install(WebLinkMicroModule(webLinkManifest))
    }
  }

  suspend fun createWebLink(webLinkManifest: WebLinkManifest): Boolean {
    deskNMM.bootstrapContext.dns.query(webLinkManifest.id)?.let { lastWebLink ->
      deskNMM.bootstrapContext.dns.uninstall(lastWebLink.id)
    }
    webLinkStore.set(webLinkManifest.id, webLinkManifest)
    deskNMM.bootstrapContext.dns.install(WebLinkMicroModule(webLinkManifest))
    return true
  }

  suspend fun removeWebLink(id: MMID): Boolean {
    deskNMM.bootstrapContext.dns.uninstall(id)
    webLinkStore.delete(id)
    return true
  }
  // 针对 WebLink 的管理部分 end

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
          PureBounds(
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
    ) = TabletopController(deskNMM, desktopServer, runningApps)
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
  }.shareIn(deskNMM.getRuntimeScope(), started = SharingStarted.Eagerly, replay = 1)

  suspend fun detail(mmid: String) {
    deskNMM.nativeFetch("file://jmm.browser.dweb/detail?app_id=$mmid")
  }

  suspend fun uninstall(mmid: String) {
    deskNMM.nativeFetch("file://jmm.browser.dweb/uninstall?app_id=$mmid")
  }

  suspend fun share(mmid: String) {
    // TODO: 分享
  }

  suspend fun search(words: String) {
    deskNMM.nativeFetch("file://web.browser.dweb/search?q=$words")
  }

  private val appSortList = DeskSortStore(deskNMM)
  suspend fun getTabletopApps(): List<DeskAppMetaData> {
    val apps =
      deskNMM.bootstrapContext.dns.search(MICRO_MODULE_CATEGORY.Application).toMutableList()
    // 简单的排序再渲染
    val sortList = appSortList.getApps()
    apps.sortBy { sortList.indexOf(it.mmid) }
    val runApps = apps.map { metaData ->
      return@map DeskAppMetaData().apply {
        running = runningApps.containsKey(metaData.mmid)
        winStates = getTabletopWindowsManager().getWindowStates(metaData.mmid)
        //...复制metaData属性
        assign(metaData.manifest)
      }
    }
    return runApps
  }

  private var preTabletopWindowsManager: TabletopWindowsManager? = null

  private val wmLock = Mutex()

  /**
   * 窗口管理器
   */
  suspend fun getTabletopWindowsManager() = wmLock.withLock {
    _desktopView.await()
    val vc = this.activity!!
    TabletopWindowsManager.getOrPutInstance(vc, IPureViewBox.from(vc)) { dwm ->
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

      preTabletopWindowsManager?.also { preDwm ->
        deskNMM.scopeLaunch(Dispatchers.Main, cancelable = true) {
          /// 窗口迁移
          preDwm.moveWindowsTo(dwm)
        }
      }
      preTabletopWindowsManager = dwm
    }
  }

  @Composable
  fun DesktopWindowsManager(content: @Composable TabletopWindowsManager.() -> Unit) {
    var windowsManager by remember { mutableStateOf<TabletopWindowsManager?>(null) }
    LaunchedEffect(Unit) {
      windowsManager = getTabletopWindowsManager()
    }
    windowsManager?.content()
  }


  private fun getDesktopUrl() = when (val url = envSwitch.get(ENV_SWITCH_KEY.DESKTOP_DEV_URL)) {
    "" -> tabletopServer.startResult.urlInfo.buildInternalUrl().build {
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


  init {
    runningApps.onChange {
      updateFlow.emit("apps")
    }
    onUpdate.filter { it != "bounds" }.collectIn(deskNMM.getRuntimeScope()) {
      upsetApps()
    }
  }
}
