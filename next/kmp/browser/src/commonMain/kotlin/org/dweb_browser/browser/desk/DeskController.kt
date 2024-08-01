package org.dweb_browser.browser.desk

import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.randomUUID

class DeskController(val deskNMM: DeskNMM.DeskRuntime) {
  val sessionId = randomUUID()
  private val vcDeferred = CompletableDeferred<IPureViewController>()
  fun setPureViewController(vc: IPureViewController) {
    vcDeferred.complete(vc)
  }

  private val isV2 by lazy { envSwitch.isEnabled(ENV_SWITCH_KEY.DESKTOP_STYLE_COMPOSE) }

  val getDesktopController = SuspendOnce {
    val vc = vcDeferred.await()
    when {
      isV2 -> DesktopV2Controller.create(deskNMM, vc)
      else -> DesktopV1Controller.create(deskNMM, vc)
    }
  }
  val getTaskbarController = SuspendOnce {
    val desktopController = getDesktopController()
    when {
      isV2 -> {
        TaskbarV2Controller(
          deskNMM = deskNMM,
          deskSessionId = sessionId,
          desktopController = desktopController,
        )
      }

      else -> TaskbarV1Controller.create(
        deskNMM = deskNMM,
        deskSessionId = sessionId,
        desktopController = desktopController,
      )
    }
  }
  val alertController = AlertController()


  suspend fun awaitReady() {
    vcDeferred.await()
  }
}