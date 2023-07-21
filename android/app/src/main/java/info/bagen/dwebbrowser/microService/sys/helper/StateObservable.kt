package info.bagen.dwebbrowser.microService.sys.helper

import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.helper.IpcEvent
import info.bagen.dwebbrowser.util.IsChange
import org.dweb_browser.helper.*

open class StateObservable(
    needFirstCall: Boolean = false,
    private val getStateJson: () -> String,
) {
    val stateChanges = IsChange(needFirstCall)
    val changeSignal = Signal<String>()
    fun observe( cb: Callback<String>) = changeSignal.listen(cb)

    val observerIpcMap = mutableMapOf<Ipc, OffListener>()
    suspend fun startObserve(ipc: Ipc) {
//        observe {
//            ipc.postMessage(
//                IpcEvent.fromUtf8(
//                    "observe",
//                    getStateJson()
//                )
//            )
//        }
    }

     fun notifyObserver() {
        runBlockingCatching {
          changeSignal.emit(getStateJson())
        }.getOrNull()
    }

    fun stopObserve(ipc: Ipc) = observerIpcMap.remove(ipc)?.let { off ->
        off(Unit)
        true
    } ?: false
}