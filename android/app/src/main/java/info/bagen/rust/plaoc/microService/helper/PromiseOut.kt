package info.bagen.rust.plaoc.microService.helper

import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture

open class PromiseOut<T : Any> {
    companion object {
        fun <T : Any> resolve(value: T) = PromiseOut<T>().also { it.resolve(value) }
        fun <T : Any> reject(e: Throwable) = PromiseOut<T>().also { it.reject(e) }
    }

    private val _future = CompletableFuture<T>()


    open fun resolve(value: T) {
        synchronized(_future) {
            _future.complete(value)
        }
    }


    open fun reject(e: Throwable) {
        synchronized(_future) {
            _future.completeExceptionally(e)
        }
    }

    val isFinished get() = synchronized(_future) { _future.isDone || _future.isCompletedExceptionally }
    val isResolved get() = synchronized(_future) { _future.isDone }
    val isRejected get() = synchronized(_future) { _future.isCompletedExceptionally }

    val value get() = synchronized(_future) { if (_future.isDone) _future.get() else null }

    suspend fun waitPromise(): T = _future.await()
}
