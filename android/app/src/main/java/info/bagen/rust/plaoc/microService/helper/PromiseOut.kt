package info.bagen.rust.plaoc.microService.helper

import kotlinx.coroutines.sync.Mutex

class PromiseOut<T : Any> {
    private val mutex = Mutex(true)
    private inline fun finish(): Boolean {
        if (mutex.isLocked) {
            mutex.unlock()
            return true
        }
        return false
    }


    private lateinit var data: T

    fun resolve(value: T) {
        if (finish()) {
            data = value
        }
    }


    private var cause: Throwable? = null
    fun reject(e: Throwable) {
        if (finish()) {
            cause = e
        }
    }


    val finished get() = !mutex.isLocked
    val resolved get() = finished && cause == null
    val rejected get() = finished && cause != null

    private suspend inline fun await() {
        if (!finished) {
            mutex.lock()// 卡住等待
            mutex.unlock()
        }
    }

    suspend fun waitPromise() {
        await()
        when (cause) {
            null -> {}
            else -> throw cause!!
        }
    }
}

