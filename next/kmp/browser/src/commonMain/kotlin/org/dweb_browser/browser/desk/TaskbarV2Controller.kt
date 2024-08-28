package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import org.dweb_browser.browser.desk.model.TaskbarAppModel
import org.dweb_browser.browser.desk.render.ITaskbarV2View
import org.dweb_browser.browser.desk.render.create
import org.dweb_browser.helper.collectIn
import org.dweb_browser.sys.window.core.helper.pickLargest
import org.dweb_browser.sys.window.core.helper.toStrict

class TaskbarV2Controller(
  deskNMM: DeskNMM.DeskRuntime,
  val deskSessionId: String,
  desktopController: DesktopControllerBase,
) : TaskbarControllerBase(deskNMM, desktopController) {
  internal val appsFlow = MutableStateFlow(emptyList<TaskbarAppModel>())

  private suspend fun upsetApps() {
    appsFlow.value = getTaskbarAppList(Int.MAX_VALUE).map { new ->
      // 必须创建一个新的TaskbarAppModel引用，否则mode的更改不会触发composable函数变更状态
      var appModel = appsFlow.value.find { it.mmid == new.mmid }?.let {
        TaskbarAppModel(it.mmid, it.icon, new.running, it.isShowClose)
      }
      if (appModel == null) {
        appModel = TaskbarAppModel(
          mmid = new.mmid,
          icon = new.icons.toStrict().pickLargest(),
          running = new.running,
          isShowClose = false,
        )
      }

      if (new.winStates.isNotEmpty()) {
        val state = new.winStates.last()
        appModel.state.apply {
          focus = state.focus
          visible = state.visible
          mode = state.mode
        }
      } else {
        appModel.state.focus = false
      }

      appModel
    }.sortedByDescending { it.state.focus }
//    updateTaskBarSize(taskbarApps.count())
  }

  internal val view = ITaskbarV2View.create(this)

  @Composable
  override fun Render() {
    view.Render()
  }

  init {
    val mmScope = deskNMM.getRuntimeScope()
    onUpdate.filter { it != "bounds" }.collectIn(mmScope) {
      upsetApps()
    }
  }
}