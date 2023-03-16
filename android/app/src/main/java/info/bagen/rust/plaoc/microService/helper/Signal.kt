package info.bagen.rust.plaoc.microService.helper

import io.ktor.util.collections.*

typealias Callback<Args> = suspend (args: Args) -> Any?
typealias SimpleCallback = Callback<Unit>
typealias OffListener = (Unit) -> Boolean

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

open class Signal<Args> {
    val listenerSet = ConcurrentSet<Callback<Args>>();
    var cpSet = setOf<Callback<Args>>()
    inline fun listen(noinline cb: Callback<Args>): OffListener {
        // TODO emit 时的cbs 应该要同步进行修改？
        listenerSet.add(cb).also {
            if (it) {
                cpSet = listenerSet.toSet()
            }
        }
        return { off(cb) }
    }

    inline fun off(noinline cb: Callback<Args>): Boolean {
        return listenerSet.remove(cb).also {
            if (it) {
                cpSet = listenerSet.toSet()
            }
        }
    }

    suspend inline fun emit(args: Args) {
        // 这里拷贝一份，避免中通对其读写的时候出问题
        val cbs = cpSet
        for (cb in cbs) {
            try {
                /// 因为 cbs 和 listenerSet 已经不是同一个列表了，所以至少说执行之前要检查一下是否还在
                if (!listenerSet.contains(cb)) {
                    continue
                }
                when (cb(args)) {
                    SIGNAL_CTOR.OFF -> listenerSet.remove(cb)
                    SIGNAL_CTOR.BREAK -> break
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun clear() {
        this.listenerSet.clear()
    }
}


class SimpleSignal : Signal<Unit>() {
    suspend fun emit() {
        emit(Unit)
    }
};
