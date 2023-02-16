package info.bagen.rust.plaoc.microService.helper

typealias Callback<Args> = (args: Args) -> Unit
typealias CallbackWithOff<Args> = (args: Args, off: OffListener) -> Unit


open class Signal<Args>() {
    private val _cbs = mutableSetOf<Callback<Args>>();

    fun listen(cb: Callback<Args>): OffListener {
        this._cbs.add(cb)
        return { off(cb) }
    }

    fun listenWithOff(cb: CallbackWithOff<Args>) {
        var off = { false }
        off = listen { args -> cb(args, off) }
    }

    fun off(cb: Callback<Args>): Boolean {
        return _cbs.remove(cb)
    }

    fun emit(args: Args) {
        for (cb in this._cbs) {
            cb(args)
        }
    }
}

typealias SimpleCallback = Callback<Unit>
typealias  OffListener = () -> Boolean

class SimpleSignal : Signal<Unit>() {
    fun emit() {
        super.emit(null as Unit);
    }
};
