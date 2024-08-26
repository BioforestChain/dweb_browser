package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.Serializable
import org.dweb_browser.browser.desk.model.DesktopAppModel
import org.dweb_browser.browser.desk.render.RenderImpl
import org.dweb_browser.browser.desk.render.layoutSaveStrategyIsMultiple
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.ext.createStore
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.platform.IPureViewController
import org.mkdesklayout.project.NFSpaceCoordinateLayout

@Serializable
data class DeskAppLayoutInfo(val screenWidth: Int, val layouts: Map<MMID, NFSpaceCoordinateLayout>)

private class DesktopV2AppLayoutStore(deskNMM: DeskNMM.DeskRuntime) {

  private val appsLayoutStore = deskNMM.createStore("apps_layout", false)

  suspend fun getStoreAppsLayouts(): List<DeskAppLayoutInfo> {
    return appsLayoutStore.getOrNull("layouts") ?: emptyList()
  }

  suspend fun setStoreAppsLayouts(layouts: List<DeskAppLayoutInfo>) {
    appsLayoutStore.set("layouts", layouts)
    appsLayoutStore.save()
  }
}

class DesktopV2Controller private constructor(
  viewController: IPureViewController,
  deskNMM: DeskNMM.DeskRuntime,
) : DesktopControllerBase(viewController, deskNMM) {
  companion object {
    suspend fun create(
      deskNMM: DeskNMM.DeskRuntime,
      viewController: IPureViewController,
    ): DesktopV2Controller {
      val controller = DesktopV2Controller(viewController, deskNMM)
      DesktopControllerBase.configSharedRoutes(controller, deskNMM)
      return controller
    }
  }

  private val openingApps = mutableSetOf<MMID>()
  internal val appsFlow = MutableStateFlow(emptyList<DesktopAppModel>())
  internal val appLayoutsFlow = MutableStateFlow(emptyList<DeskAppLayoutInfo>())

  private val appsLayoutStore = DesktopV2AppLayoutStore(deskNMM)

  private suspend fun getAllAppLayouts() = appsLayoutStore.getStoreAppsLayouts()

  suspend fun updateAppsLayouts(screenWidth: Int, layouts: Map<MMID, NFSpaceCoordinateLayout>) {
    val allLayouts = appsLayoutStore.getStoreAppsLayouts().toMutableList()
    val isMultiple = layoutSaveStrategyIsMultiple()
    if (isMultiple) {
      allLayouts.removeAll { it.screenWidth == screenWidth }
    } else {
      allLayouts.clear()
    }
    allLayouts.add(DeskAppLayoutInfo(screenWidth, layouts))
    appLayoutsFlow.value = allLayouts
    appsLayoutStore.setStoreAppsLayouts(allLayouts)
  }

  private suspend fun upsetApps() {
    appsFlow.value = getDesktopApps().map { appMetaData ->
      val runStatus = if (appMetaData.running) {
        openingApps.remove(appMetaData.mmid)
        DesktopAppModel.DesktopAppRunStatus.Opened
      } else if (openingApps.contains(appMetaData.mmid)) {
        DesktopAppModel.DesktopAppRunStatus.Opening
      } else {
        DesktopAppModel.DesktopAppRunStatus.Close
      }

      appsFlow.value.find { oldApp ->
        oldApp.mmid == appMetaData.mmid
      }?.also { it.running = runStatus } ?: DesktopAppModel(
        appMetaData = appMetaData, initRunningState = runStatus
      )
    }
    appLayoutsFlow.value = getAllAppLayouts()
  }

  override suspend fun openAppOrActivate(mmid: MMID) {
    val app = appsFlow.value.find { it.mmid == mmid } ?: return
    when (val webLink = app.webLink) {
      null -> {
        if (app.running == DesktopAppModel.DesktopAppRunStatus.Close) {
          app.running = DesktopAppModel.DesktopAppRunStatus.Opening
          openingApps.add(mmid)
        }
        // 激活应用
        super.openAppOrActivate(mmid)
      }

      else -> {
        deskNMM.nativeFetch(webLink)
//        deskNMM.connect(app.mmid).request(PureClientRequest(webLink, PureMethod.GET))
      }
    }
  }

  override suspend fun closeApp(mmid: MMID) {
    openingApps.remove(mmid)
    super.closeApp(mmid)
  }

  @Composable
  override fun Render() {
    RenderImpl()
  }

  init {
    onUpdate.filter { it != "bounds" }.collectIn(deskNMM.getRuntimeScope()) {
      upsetApps()
    }
  }
}