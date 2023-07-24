package info.bagen.dwebbrowser.microService.desktop

import androidx.compose.runtime.Stable
import info.bagen.dwebbrowser.microService.browser.jmm.EIpcEvent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.AppMetaData
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.microservice.ipc.helper.IpcEvent

@Stable
class DesktopController(private val microModule: DesktopNMM) {

  private var activityTask = PromiseOut<DesktopActivity>()
  suspend fun waitActivityCreated() = activityTask.waitPromise()

  var activity: DesktopActivity? = null
    set(value) {
      if (field == value) {
        return
      }
      field = value
      if (value == null) {
        activityTask = PromiseOut()
      } else {
        activityTask.resolve(value)
      }
    }

  private val openLock = Mutex()
  suspend fun openApp(appMetaData: AppMetaData) {
    openLock.withLock {
      val (ipc) = microModule.bootstrapContext.dns.connect(appMetaData.id)
      debugDesktop("openApp", "postMessage==>activity ${ipc.remote.mmid}")
      ipc.postMessage(IpcEvent.fromUtf8(EIpcEvent.Activity.event, ""))
    }
  }
}