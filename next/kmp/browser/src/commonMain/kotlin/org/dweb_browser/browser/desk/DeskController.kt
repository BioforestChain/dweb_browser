package org.dweb_browser.browser.desk

import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.channelRequest
import org.dweb_browser.helper.ChangeState
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.pure.http.PureTextFrame

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
  val appSortList = DeskSortStore(deskNMM).also { appSortList ->
    deskNMM.scopeLaunch(cancelable = true) {
      suspend fun doObserve(urlPath: String, cb: suspend ChangeState<MMID>.() -> Unit) {
        val response = deskNMM.channelRequest(urlPath) {
          for (frame in income) {
            when (frame) {
              is PureTextFrame -> {
                Json.decodeFromString<ChangeState<MMID>>(frame.text).also {
                  it.cb()
                }
              }

              else -> {}
            }
          }
        }
        debugDesk("doObserve error", response.status)
      }
      // app排序
      doObserve("file://dns.std.dweb/observe/install-apps") {
        // 强制触发一次变更
        deskNMM.runningApps = deskNMM.runningApps.toMap()
        // 对排序app列表进行更新
        removes.map {
          appSortList.delete(it)
        }
        adds.map {
          if (!appSortList.has(it)) {
            appSortList.push(it)
          }
        }
      }
    }
  }

  suspend fun awaitReady() {
    vcDeferred.await()
  }
}