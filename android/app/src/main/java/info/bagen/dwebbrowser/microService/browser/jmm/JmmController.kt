package info.bagen.dwebbrowser.microService.browser.jmm

import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.message.IpcEvent
import org.dweb_browser.microservice.help.Mmid
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class EIpcEvent(val event:String){
  State("state"),
  Ready("ready"),
  Activity("activity"),
  Close("close")
}

class JmmController(private val jmmNMM: JmmNMM) {

  private val openIpcMap = mutableMapOf<Mmid, Ipc>()
  private val openLock = Mutex()

  suspend fun openApp(mmid: Mmid) {
    openLock.withLock {
      openIpcMap.getOrPut(mmid) {
        val (ipc) = jmmNMM.connect(mmid)
        ipc.onEvent {
          debugJMM(
            "openApp",
            "event::${it.event.name}==>${it.event.data}  from==> $mmid "
          )
          if (it.event.name == EIpcEvent.Close.event) {
            openIpcMap.remove(mmid)
          }
        }
        ipc
      }.also { ipc ->
        debugJMM("openApp", "postMessage==>activity ${ipc.remote.mmid}")
        ipc.postMessage(IpcEvent.fromUtf8(EIpcEvent.Activity.event, ""))
      }
    }
  }

  suspend fun closeApp(mmid: Mmid) {
    openIpcMap[mmid]?.let { ipc ->
      debugJMM("close APP", "postMessage==>close  $mmid, ${ipc.remote.mmid}")
      ipc.postMessage(IpcEvent.fromUtf8(EIpcEvent.Close.event, ""))
      openIpcMap.remove(mmid)
    }
  }

}