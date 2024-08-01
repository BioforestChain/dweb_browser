package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import org.dweb_browser.browser.desk.render.FloatBarState
import org.dweb_browser.browser.desk.types.DeskAppMetaData
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.SafeHashSet

sealed class TaskbarControllerBase(
  internal val deskNMM: DeskNMM.DeskRuntime,
  internal val desktopController: DesktopControllerBase,
) {
  val state = FloatBarState()
  protected val taskbarStore = TaskbarStore(deskNMM)

  init {
    /**
     * 绑定 runningApps 集合
     */
    deskNMM.scopeLaunch(cancelable = true) {
      var apps = getTaskbarShowAppList()
      deskNMM.runningAppsFlow.collect { runningApps ->
        for (mmid in apps) {
          if (!runningApps.containsKey(mmid)) {
            apps.remove(mmid)
          }
        }
        apps = (runningApps.keys.toList() + apps).distinct().toMutableList()

        // 只展示4个，结合返回桌面的一个tarBar有5个图标
//        if (apps.size > 4) {
//          apps = apps.subList(0, 3)
//        }
        /// 保存到数据库
        taskbarStore.setApps(apps)
      }
    }
  }

  /** 展示在taskbar中的应用列表 */
  protected suspend fun getTaskbarShowAppList() = taskbarStore.getApps().toMutableList()
  protected suspend fun getFocusApp() = getTaskbarShowAppList().firstOrNull()
  fun open(mmid: MMID) = deskNMM.scopeLaunch(cancelable = true) {
    deskNMM.open(mmid)
  }

  fun quit(mmid: MMID) = deskNMM.scopeLaunch(cancelable = true) {
    deskNMM.quit(mmid)
  }

  // 触发状态更新
  protected val stateFlow = MutableSharedFlow<TaskbarV1Controller.TaskBarState>()
  val onStatus = stateFlow.asSharedFlow()
  fun toggleFloatWindow(openTaskbar: Boolean?) = deskNMM.scopeAsync(cancelable = true) {
    val toggle = openTaskbar ?: !state.floatActivityState
    // 监听状态是否是float
    getFocusApp()?.let { focusApp ->
      stateFlow.emit(TaskbarV1Controller.TaskBarState(toggle, focusApp))
    }
    if (toggle) openTaskbarActivity() else closeTaskbarActivity()
  }

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

  fun toggleWindowMaximize(mmid: MMID) = deskNMM.scopeAsync(cancelable = true) {
    desktopController.getDesktopWindowsManager().toggleMaximize(mmid)
  }

  /**
   * 将其它视图临时最小化到 TaskbarView/TooggleDesktopButton 按钮里头，在此点击该按钮可以释放这些临时视图到原本的状态
   */
  fun toggleDesktopView() = deskNMM.scopeLaunch(cancelable = true) {
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

  internal suspend fun getTaskbarAppList(limit: Int): List<DeskAppMetaData> {
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
          running = deskNMM.runningApps.contains(appId)
          winStates = desktopController.getDesktopWindowsManager().getWindowStates(metaData.mmid)
          //...复制metaData属性
          assign(metaData.manifest)
        }
      }
    }

    return apps.values.toList()
  }

  @Composable
  abstract fun Render()
}