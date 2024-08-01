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
      appsFlow.value.find { it.mmid == new.mmid }?.also { it.running = new.running }
        ?: TaskbarAppModel(
          mmid = new.mmid,
          icon = new.icons.toStrict().pickLargest(),
          running = new.running,
          isShowClose = false,
        )
    }
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