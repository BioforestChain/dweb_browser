package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import org.dweb_browser.browser.desk.types.DeskAppMetaData
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.build
import org.dweb_browser.helper.envSwitch
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.resolvePath


class TaskbarController private constructor(
  val deskNMM: DeskNMM.DeskRuntime,
  val deskSessionId: String,
  private val desktopController: DesktopController,
  private val taskbarServer: HttpDwebServer,
  private val runningApps: ChangeableMap<MMID, RunningApp>,
) {
  internal val state = TaskbarState();

  companion object {

    suspend fun create(
      deskSessionId: String,
      deskNMM: DeskNMM.DeskRuntime,
      desktopController: DesktopController,
      taskbarServer: HttpDwebServer,
      runningApps: ChangeableMap<MMID, RunningApp>,
    ) =
      TaskbarController(
        deskNMM,
        deskSessionId,
        desktopController,
        taskbarServer,
        runningApps
      )
  }

  private val _taskbarView =
    deskNMM.scopeAsync(cancelable = true) { ITaskbarView.create(this@TaskbarController) }

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
  internal val updateSignal = SimpleSignal()
  val onUpdate = updateSignal.toListener()

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
        updateSignal.emit()
      }
    }
    // 监听窗口状态改变
    desktopController.onUpdate {
      updateSignal.emit()
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
    state.layoutWidth = reSize.width
    state.layoutHeight = reSize.height
  }

  /**
   * 将其它视图临时最小化到 TaskbarView/TooggleDesktopButton 按钮里头，在此点击该按钮可以释放这些临时视图到原本的状态
   */
  suspend fun toggleDesktopView() {
    val allWindows = desktopController.getDesktopWindowsManager().allWindows.keys.toList()
    if (allWindows.find { it.isVisible() } != null) {
      allWindows.forEach { win ->
        win.toggleVisible(false)
      }
    } else {
      allWindows.forEach { win ->
        win.toggleVisible(true)
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

  fun getTaskbarUrl() =
    taskbarServer.startResult.urlInfo.buildInternalUrl().build {
      resolvePath("/taskbar.html")
    }

  fun getTaskbarDWebViewOptions() = DWebViewOptions(
    url = getTaskbarUrl().toString(),
    openDevTools = envSwitch.has("taskbar-devtools"),
    privateNet = true,
    detachedStrategy = DWebViewOptions.DetachedStrategy.Ignore,
    tag = 2,
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
