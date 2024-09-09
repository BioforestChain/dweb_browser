package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.Serializable
import org.dweb_browser.browser.desk.model.DesktopAppModel
import org.dweb_browser.browser.desk.render.NFSpaceCoordinateLayout
import org.dweb_browser.browser.desk.render.RenderImpl
import org.dweb_browser.browser.desk.render.layoutSaveStrategyIsMultiple
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.ext.createStore
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.helper.platform.IPureViewController

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

  // 存放v2拖拽排序的位置信息。
  internal val appsLayout by lazy {
    val isCustomLayout = envSwitch.isEnabled(ENV_SWITCH_KEY.DESKTOP_CUSTOM_LAYOUT)
    when {
      isCustomLayout -> DesktopV2AppLayoutController(deskNMM, this)
      else -> null
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

    appsLayout?.clearInvalidLayouts(apps.map { it.mmid })
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

  override suspend fun remove(mmid: MMID, isWebLink: Boolean) {
    appsLayout?.removeLayouts(mmid)
    super.remove(mmid, isWebLink)
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

@Serializable
data class DeskAppLayoutInfo(val screenWidth: Int, val layouts: Map<MMID, NFSpaceCoordinateLayout>)

internal class DesktopV2AppLayoutController(
  deskNMM: DeskNMM.DeskRuntime,
  private val controller: DesktopV2Controller,
) {
  internal val appLayoutsFlow = MutableStateFlow(emptyList<DeskAppLayoutInfo>())
  suspend fun updateAppsLayouts(screenWidth: Int, layouts: Map<MMID, NFSpaceCoordinateLayout>) {
    val allLayouts = getStoreAppsLayouts().toMutableList()
    val isMultiple = layoutSaveStrategyIsMultiple()
    if (isMultiple) {
      allLayouts.removeAll { it.screenWidth == screenWidth }
    } else {
      allLayouts.clear()
    }
    allLayouts.add(DeskAppLayoutInfo(screenWidth, layouts))
    appLayoutsFlow.value = allLayouts
    setStoreAppsLayouts(allLayouts)
  }

  private val appsLayoutStore = deskNMM.createStore("apps_layout", false)

  suspend fun getStoreAppsLayouts(): List<DeskAppLayoutInfo> {
    return appsLayoutStore.getOrNull("layouts") ?: emptyList()
  }

  suspend fun setStoreAppsLayouts(layouts: List<DeskAppLayoutInfo>) {
    appsLayoutStore.set("layouts", layouts)
  }

  suspend fun removeLayouts(mmid: MMID) {
    val result = getStoreAppsLayouts().map { layoutInfo ->
      layoutInfo.copy(layouts = layoutInfo.layouts.filter { layout ->
        layout.key != mmid
      })
    }
    setStoreAppsLayouts(result)
  }

  suspend fun clearInvalidLayouts(list: List<MMID>) {
    val result = getStoreAppsLayouts().map { layoutInfo ->
      layoutInfo.copy(layouts = layoutInfo.layouts.filter { layout ->
        list.contains(layout.key)
      })
    }
    setStoreAppsLayouts(result)
    appLayoutsFlow.value = result
  }
}