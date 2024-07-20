package org.dweb_browser.browser.desk.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import kotlinx.coroutines.flow.filter
import org.dweb_browser.browser.desk.DesktopController
import org.dweb_browser.browser.desk.model.DesktopAppData
import org.dweb_browser.browser.desk.model.DesktopAppModel
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.sys.window.core.helper.pickLargest
import org.dweb_browser.sys.window.core.helper.toStrict

@Composable
internal fun rememberDesktopApps(
  desktopController: DesktopController,
): MutableList<DesktopAppModel> {
  return produceState(mutableListOf<DesktopAppModel>()) {
    desktopController.onUpdate.filter { it != "bounds" }.collect {
      value = desktopController.getDesktopApps().map {
        val icon = it.icons.toStrict().pickLargest()

        val isSystemApp = it.targetType == "nmm"
        //TODO: 临时的处理。等待分拆weblink后再优化。
        val isWebLink =
          it.categories.contains(MICRO_MODULE_CATEGORY.Web_Browser) && it.mmid != "web.browser.dweb"

        val runStatus = if (it.running) {
          desktopController.toRunningApps.remove(it.mmid)
          DesktopAppModel.DesktopAppRunStatus.RUNNING
        } else if (desktopController.toRunningApps.contains(it.mmid)) {
          DesktopAppModel.DesktopAppRunStatus.TORUNNING
        } else {
          DesktopAppModel.DesktopAppRunStatus.NONE
        }

        val oldApp = value.find { oldApp ->
          oldApp.mmid == it.mmid
        }

        oldApp?.copy(running = runStatus) ?: DesktopAppModel(
          name = it.short_name.ifEmpty { it.name },
          mmid = it.mmid,
          data = if (isWebLink) DesktopAppData.WebLink(
            mmid = it.mmid,
            url = it.name
          ) else DesktopAppData.App(mmid = it.mmid),
          icon = icon,
          isSystemApp = isSystemApp,
          running = runStatus,
        )
      }.toMutableList()
    }
  }.value
}