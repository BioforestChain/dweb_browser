package info.bagen.dwebbrowser.microService.sys.jmm

import info.bagen.dwebbrowser.microService.helper.EIpcEvent
import info.bagen.dwebbrowser.microService.helper.Mmid
import info.bagen.dwebbrowser.microService.ipc.Ipc
import info.bagen.dwebbrowser.microService.ipc.IpcEvent

class JmmController(private val jmmNMM: JmmNMM) {

  private val openIPCMap = mutableMapOf<Mmid, Ipc>()
  suspend fun openApp(mmid: Mmid) {
    openIPCMap.getOrPut(mmid) {
      val (ipc) = jmmNMM.connect(mmid)
      ipc.onEvent {
        if (it.event.name == EIpcEvent.Ready.event) { // webview加载完成，可以隐藏加载框
          debugJMM("openApp", "event::${it.event.name}==>${it.event.data}  from==> $mmid ")
        }
      }
      ipc
    }.also { ipc ->
      debugJMM("openApp", "postMessage==>activity  $mmid, $ipc")
      ipc.postMessage(IpcEvent.fromUtf8(EIpcEvent.Activity.event, ""))
    }
  }
}