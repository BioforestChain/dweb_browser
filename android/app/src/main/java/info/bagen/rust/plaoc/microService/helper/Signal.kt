package info.bagen.rust.plaoc.microService.helper

typealias Callback<Args> = suspend (args: Args) -> Any?
typealias SimpleCallback = suspend () -> Any?
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
        _cbs.add(cb)
        return { off(cb) }
    }

    private fun off(cb: Callback<Args>): Boolean {
        return _cbs.remove(cb)
    }

    suspend fun emit(args: Args) {

        val iter = this._cbs.iterator()
        for (cb in iter) {
            try {
                when (cb(args)) {
                    SIGNAL_CTOR.OFF -> iter.remove()
                    SIGNAL_CTOR.BREAK -> break
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}


class SimpleSignal {
    private val _cbs = mutableSetOf<SimpleCallback>();

    fun listen(cb: SimpleCallback): OffListener {
        this._cbs.add(cb)
        return { off(cb) }
    }

    fun off(cb: SimpleCallback): Boolean {
        return _cbs.remove(cb)
    }

    suspend fun emit() {
        val iter = this._cbs.iterator()
        for (cb in iter) {
            when (cb()) {
                SIGNAL_CTOR.OFF -> iter.remove()
                SIGNAL_CTOR.BREAK -> break
            }
        }
    }
};
