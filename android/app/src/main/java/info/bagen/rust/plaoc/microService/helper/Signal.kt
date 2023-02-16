package info.bagen.rust.plaoc.microService.helper

import info.bagen.rust.plaoc.microService.ipc.Ipc


fun <T:Any> createSignal():Signal<T> {
    return Signal()
}

class Signal<Callback: Any>() {
    var acc = 0;
    private val _cbs = mutableMapOf<Number,Callback>();
    fun listen(cb: Callback): Number {
        this._cbs[acc++] = cb
       return acc
    }

    fun deleteCbs(acc:Number) {
        this._cbs.remove(acc)
    }

   fun emit(args: IpcMessage?, ipc: Ipc?) {
       if(args == null || ipc == null) {
           _cbs.forEach {
              deleteCbs(it.key)
           }
       }
        for (cb in this._cbs) {
            cb.let { args }
        }
    }
}