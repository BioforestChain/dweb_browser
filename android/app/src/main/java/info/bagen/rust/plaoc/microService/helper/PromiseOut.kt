package info.bagen.rust.plaoc.microService.helper

import kotlinx.coroutines.sync.Mutex

class PromiseOut<T : Any> {
    companion object {
        fun <T : Any> resolve(value: T) = PromiseOut<T>().also { it.resolve(value) }
        fun <T : Any> reject(e: Throwable) = PromiseOut<T>().also { it.reject(e) }
//        fun reject(e: Throwable) = PromiseOut<Unit>().also { it.reject(e) }
    }

    private val mutex = Mutex(true)
    private inline fun finish(): Boolean {
        if (mutex.isLocked) {
            mutex.unlock()
            return true
        }
        return false
    }


    private lateinit var _value: T

    fun resolve(value: T) {
        if (finish()) {
            _value = value
        }
    }


    private var _cause: Throwable? = null
    fun reject(e: Throwable) {
        if (finish()) {
            _cause = e
        }
    }


    val finished get() = !mutex.isLocked
    val resolved get() = finished && _cause == null
    val rejected get() = finished && _cause != null
    val value get() = if (resolved) _value else null
    val cause get() = _cause

    private suspend inline fun await() {
        if (!finished) {
            mutex.lock()// 卡住等待
            mutex.unlock()
        }
    }

    suspend fun waitPromise(): T {
        await()
        return when (_cause) {
            null -> _value
            else -> throw _cause!!
        }
    }
}

