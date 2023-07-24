package info.bagen.dwebbrowser.microService.browser.jmm

import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.helper.IpcEvent
import org.dweb_browser.helper.Mmid
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class EIpcEvent(val event:String){
  State("state"),
  Ready("ready"),
  Activity("activity"),
  Close("close")
}

class JmmController(private val jmmNMM: JmmNMM) {

  private val openLock = Mutex()

  suspend fun openApp(mmid: Mmid) {
    openLock.withLock {
        val (ipc) = jmmNMM.bootstrapContext.dns.connect(mmid)
      debugJMM("openApp", "postMessage==>activity ${ipc.remote.mmid}")
      ipc.postMessage(IpcEvent.fromUtf8(EIpcEvent.Activity.event, ""))
    }
  }

  suspend fun closeApp(mmid: Mmid) {
      debugJMM("close APP", "postMessage==>close  $mmid")
      jmmNMM.bootstrapContext.dns.close(mmid)
  }

}