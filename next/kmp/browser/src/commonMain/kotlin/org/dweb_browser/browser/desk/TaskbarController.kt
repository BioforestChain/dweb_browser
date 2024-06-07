package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.ktor.http.Url
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.serialization.Serializable
import org.dweb_browser.browser.desk.types.DeskAppMetaData
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.ENV_SWITCH_KEY
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.SafeHashSet
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.build
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.envSwitch
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.resolvePath


class TaskbarController private constructor(
  override val deskNMM: DeskNMM.DeskRuntime,
  val deskSessionId: String,
  val desktopController: DesktopController,
  val taskbarServer: HttpDwebServer,
  private val runningApps: ChangeableMap<MMID, RunningApp>,
): DesktopAppController(deskNMM) {
  internal val state = TaskbarState();

  companion object {

    suspend fun create(
      deskSessionId: String,
      deskNMM: DeskNMM.DeskRuntime,
      desktopController: DesktopController,
      taskbarServer: HttpDwebServer,
      runningApps: ChangeableMap<MMID, RunningApp>,
    ) = TaskbarController(
      deskNMM, deskSessionId, desktopController, taskbarServer, runningApps
    )
  }

  private val _taskbarView = deskNMM.scopeAsync(cancelable = true) {
    ITaskbarView.create(this@TaskbarController).also { taskbarView ->
      deskNMM.onBeforeShutdown {
        deskNMM.scopeLaunch(cancelable = false) {
          taskbarView.taskbarDWebView.destroy()
        }
      }
    }
  }

  private suspend fun taskbarView() = _taskbarView.await()

  @Composable
  fun TaskbarView(content: @Composable ITaskbarView.() -> Unit) {
    var view by remember { mutableStateOf<ITaskbarView?>(null) }
    LaunchedEffect(this) {
      view = taskbarView()
    }
    view?.content()
  }

  private val taskbarStore = TaskbarStore(deskNMM)

  /** 展示在taskbar中的应用列表 */
  private suspend fun getTaskbarShowAppList() = taskbarStore.getApps()
  private suspend fun getFocusApp() = getTaskbarShowAppList().firstOrNull()
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

  // 触发状态更新
  private val stateSignal = Signal<TaskBarState>()
  val onStatus = stateSignal.toListener()

  init {
    /**
     * 绑定 runningApps 集合
     */
    deskNMM.scopeLaunch(cancelable = true) {
      val apps = getTaskbarShowAppList()
      runningApps.onChange { map ->
        /// 将新增的打开应用追加到列表签名
        for (mmid in map.origin.keys) {
          apps.remove(mmid)
          apps.add(0, mmid) // 追加到第一个
        }
        for (remove in map.removes) {
          apps.remove(remove)
        }
        // 只展示4个，结合返回桌面的一个tarBar有5个图标
        if (apps.size > 4) {
          apps.removeLastOrNull()
        }
        /// 保存到数据库
        taskbarStore.setApps(apps)
        // 窗口打开时触发
        updateFlow.emit("apps")
      }
    }
    // 监听窗口状态改变
    desktopController.onUpdate.collectIn(deskNMM.getRuntimeScope()) {
      updateFlow.emit(it)
    }
  }

  suspend fun getTaskbarAppList(limit: Int): List<DeskAppMetaData> {
    val apps = mutableMapOf<MMID, DeskAppMetaData>()
    for (appId in getTaskbarShowAppList()) {
      if (apps.size >= limit) {
        break
      }
      if (appId == deskNMM.mmid || apps.containsKey(appId)) {
        continue
      }
      val metaData = deskNMM.bootstrapContext.dns.query(appId)
      if (metaData != null) {
        apps[appId] = DeskAppMetaData().apply {
          running = runningApps.contains(appId)
          winStates = desktopController.getDesktopWindowsManager().getWindowStates(metaData.mmid)
          //...复制metaData属性
          assign(metaData.manifest)
        }
      }
    }

    return apps.values.toList()
  }

  /**
   * 对Taskbar自身进行resize
   * 根据web元素的大小进行自适应调整
   *
   * @returns 如果视图发生了真实的改变（不论是否变成说要的结果），则返回 true
   */
  fun resize(reSize: ReSize) {
    // TODO: Mike 需要处理掉，防止web影响正常的taskbar大小。
//    state.layoutWidth = reSize.width
//    state.layoutHeight = reSize.height
  }

  fun toggleDragging(dragging: Boolean): Boolean {
    state.taskbarDragging = dragging
    return dragging
  }

  /**
   * 将其它视图临时最小化到 TaskbarView/TooggleDesktopButton 按钮里头，在此点击该按钮可以释放这些临时视图到原本的状态
   */
  suspend fun toggleDesktopView() {
    val windowsManager = desktopController.getDesktopWindowsManager()
    val allWindows = windowsManager.allWindowsFlow.value.keys.toList()
    if (allWindows.isEmpty() || allWindows.find { it.isVisible } != null) {
      allWindows.map { win ->
        win.hide()
      }
      windowsManager.focusDesktop()
    } else {
      allWindows.map { win ->
        win.show()
      }
    }
  }

  private var activityTask = PromiseOut<IPureViewBox>()
  suspend fun waitActivityCreated() = activityTask.waitPromise()

  var platformContext: IPureViewBox? = null
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

  fun getTaskbarUrl() = when (val url = envSwitch.get(ENV_SWITCH_KEY.TASKBAR_DEV_URL)) {
    "" -> taskbarServer.startResult.urlInfo.buildInternalUrl().build {
      resolvePath("/taskbar.html")
    }

    else -> Url(url)
  }


  fun getTaskbarDWebViewOptions() = DWebViewOptions(
    url = getTaskbarUrl().toString(),
    openDevTools = envSwitch.isEnabled(ENV_SWITCH_KEY.TASKBAR_DEVTOOLS),
    privateNet = true,
    // 这里使用离屏渲染，来确保能在jDialog中透明背景地显示
    // 现在离屏渲染还有很严重的BUG没有修复，所以这里谨慎使用，只用在taskbar这种没有输入框的简单模块中
    enabledOffScreenRender = !envSwitch.isEnabled(ENV_SWITCH_KEY.DWEBVIEW_ENABLE_TRANSPARENT_BACKGROUND),
    detachedStrategy = DWebViewOptions.DetachedStrategy.Ignore,
    viewId = 2,
    subDataDirName = "taskbar"
  )

  @Serializable
  data class ReSize(val width: Float, val height: Float)

  @Serializable
  data class TaskBarState(val focus: Boolean, val appId: String)


  /**
   * 打开悬浮框
   */
  private fun openTaskbarActivity() = if (!state.floatActivityState) {
    state.floatActivityState = true
    true
  } else false

  private fun closeTaskbarActivity() = if (state.floatActivityState) {
    state.floatActivityState = false
    true
  } else false

  suspend fun toggleFloatWindow(openTaskbar: Boolean?): Boolean {
    val toggle = openTaskbar ?: !state.floatActivityState
    // 监听状态是否是float
    getFocusApp()?.let { focusApp ->
      stateSignal.emit(
        TaskBarState(toggle, focusApp)
      )
    }
    return if (toggle) openTaskbarActivity() else closeTaskbarActivity()
  }
}
