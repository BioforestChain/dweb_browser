package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import org.dweb_browser.browser.desk.model.DesktopAppModel
import org.dweb_browser.browser.desk.render.RenderImpl
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.collectIn
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
  }

  override suspend fun open(mmid: MMID) {
    val app = appsFlow.value.find { it.mmid == mmid } ?: return
    when (val webLink = app.webLink) {
      null -> {
        if (app.running == DesktopAppModel.DesktopAppRunStatus.Close) {
          app.running = DesktopAppModel.DesktopAppRunStatus.Opening
          openingApps.add(mmid)
        }
        // 不论如何都进行 open，因为这本质是 openAppOrActivate
        super.open(mmid)
      }

      else -> {
        deskNMM.nativeFetch(webLink)
//        deskNMM.connect(app.mmid).request(PureClientRequest(webLink, PureMethod.GET))
      }
    }
  }

  override suspend fun quit(mmid: MMID) {
    openingApps.remove(mmid)
    super.quit(mmid)
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