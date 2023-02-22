package info.bagen.rust.plaoc.microService.helper

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PromiseOut<T : Any> {
    companion object {
        fun <T : Any> resolve(value: T) = PromiseOut<T>().also { it.resolve(value) }
        fun <T : Any> reject(e: Throwable) = PromiseOut<T>().also { it.reject(e) }
//        fun reject(e: Throwable) = PromiseOut<Unit>().also { it.reject(e) }
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


    private var _finished = false
    private val mutex = Mutex(true)// 我们不能用 mutex.isLocked 来替代 _finished，因为它有可能同时被多个线程所处置
    private inline fun finish(): Boolean {
        if (!_finished) {
            _finished = true
            mutex.unlock()
            return true
        }
        return false
    }

    val isFinished get() = _finished
    val isResolved get() = _finished && _cause == null
    val isRejected get() = _finished && _cause != null
    val value get() = if (isResolved) _value else null
    val cause get() = _cause

    private suspend inline fun await() {
        if (!_finished) {
            mutex.lock() // 卡住等待
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

