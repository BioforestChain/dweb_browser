package info.bagen.rust.plaoc.microService.ipc.helper

import info.bagen.rust.plaoc.microService.ipc.Ipc



fun <Callback: ipcSignal> createSignal(): Signal<Callback> {
    return Signal()
}

typealias ipcSignal = (args: args) -> Unit
typealias args = MutableList<Any>

class Signal<Callback: ipcSignal>() {
    private val _cbs = mutableSetOf<Callback>();
    fun listen(cb: Callback): () -> Boolean {
        this._cbs.add(cb)
       return {
           _cbs.remove(cb)
       }
    }
   fun emit(args: IpcMessage?, ipc: Ipc?) {
        for (cb in this._cbs) {
            cb.let { args }
        }
    }
}