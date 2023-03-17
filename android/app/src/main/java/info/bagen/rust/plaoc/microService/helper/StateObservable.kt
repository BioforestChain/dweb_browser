package info.bagen.rust.plaoc.microService.helper

import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.IpcEvent
import info.bagen.rust.plaoc.util.IsChange

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