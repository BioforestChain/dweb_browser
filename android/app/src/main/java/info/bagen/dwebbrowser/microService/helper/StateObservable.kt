package info.bagen.dwebbrowser.microService.helper

import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.message.IpcEvent
import info.bagen.dwebbrowser.util.IsChange
import org.dweb_browser.helper.*

open class StateObservable(
    needFirstCall: Boolean = false,
    private val getStateJson: () -> String,
) {
    val stateChanges = IsChange(needFirstCall)
    val changeSignal = SimpleSignal()
    inline fun observe(noinline cb: SimpleCallback) = changeSignal.listen(cb)

    val observerIpcMap = mutableMapOf<Ipc, OffListener>()
    suspend fun startObserve(ipc: Ipc) {
        observerIpcMap.getOrPut(ipc) {
            observe {
                ipc.postMessage(
                    IpcEvent.fromUtf8(
                        "observe",
                        getStateJson()
                    )
                )
            }
        }
    }

    inline fun notifyObserver() {
        runBlockingCatching {
          changeSignal.emit()
        }.getOrNull()
    }

    fun stopObserve(ipc: Ipc) = observerIpcMap.remove(ipc)?.let { off ->
        off(Unit)
        true
    } ?: false
}