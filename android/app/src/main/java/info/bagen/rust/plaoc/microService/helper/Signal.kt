package info.bagen.rust.plaoc.microService.helper

typealias Callback<Args> = (args: Args) -> Any?
typealias SimpleCallback = Callback<Unit>
typealias OffListener = () -> Boolean

/** 控制器 */
enum class SIGNAL_CTOR {
    /**
     * 返回该值，会解除监听
     */
    OFF,

    /**
     * 返回该值，会让接下来的其它监听函数不再触发
     */
    BREAK,
    ;
}

open class Signal<Args>() {
    private val _cbs = mutableSetOf<Callback<Args>>();

    fun listen(cb: Callback<Args>): OffListener {
        this._cbs.add(cb)
        return { off(cb) }
    }

    fun off(cb: Callback<Args>): Boolean {
        return _cbs.remove(cb)
    }

    fun emit(args: Args) {
        val iter = this._cbs.iterator()
        for (cb in iter) {
            when (cb(args)) {
                SIGNAL_CTOR.OFF -> iter.remove()
                SIGNAL_CTOR.BREAK -> break
            }
        }
    }
}


class SimpleSignal : Signal<Unit>() {
    fun emit() {
        super.emit(null as Unit);
    }
};
