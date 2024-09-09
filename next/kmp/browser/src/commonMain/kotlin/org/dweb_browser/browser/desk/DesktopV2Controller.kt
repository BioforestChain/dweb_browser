package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import org.dweb_browser.browser.desk.model.DesktopAppModel
import org.dweb_browser.browser.desk.render.RenderImpl
import org.dweb_browser.browser.desk.render.layoutSaveStrategyIsMultiple
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.browser.desk.render.NFSpaceCoordinateLayout

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

  suspend fun updateAppsLayouts(screenWidth: Int, layouts: Map<MMID, NFSpaceCoordinateLayout>) {
    if (isCustomLayout) {
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
  }

  private suspend fun upsetApps() {
    val oldApps = appsFlow.value
    val apps = getDesktopApps().map { appMetaData ->
      val runStatus = when {
        appMetaData.running -> {
          openingApps.remove(appMetaData.mmid)
          DesktopAppModel.DesktopAppRunStatus.Opened
        }

        openingApps.contains(appMetaData.mmid) -> DesktopAppModel.DesktopAppRunStatus.Opening
        else -> DesktopAppModel.DesktopAppRunStatus.Close
      }

      oldApps.find { oldApp ->
        oldApp.mmid == appMetaData.mmid
      }?.also { it.running = runStatus } ?: DesktopAppModel(
        appMetaData = appMetaData, initRunningState = runStatus
      )
    }

    if (isCustomLayout) {
      appsLayoutStore.clearInvaildLayouts(apps.map { it.mmid })
      appLayoutsFlow.value = appsLayoutStore.getStoreAppsLayouts()
    }

    appsFlow.value = apps
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