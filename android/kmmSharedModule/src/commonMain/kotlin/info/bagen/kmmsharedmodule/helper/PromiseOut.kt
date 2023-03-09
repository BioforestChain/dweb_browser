package info.bagen.kmmsharedmodule.helper

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi

class PromiseOut<T : Any> {
    companion object {
        fun <T : Any> resolve(value: T) = PromiseOut<T>().also { it.resolve(value) }
        fun <T : Any> reject(e: Throwable) = PromiseOut<T>().also { it.reject(e) }
    }

    private val _future = CompletableDeferred<T>()


    fun resolve(value: T) {
        _future.complete(value)
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    fun reject(e: Throwable) {
        _future.getCompletionExceptionOrNull()

        _future.completeExceptionally(e)
    }

    val isFinished get() = _future.isCompleted || _future.isCancelled
    val isResolved get() = _future.isCompleted
    val isRejected get() = _future.isCancelled

    @OptIn(ExperimentalCoroutinesApi::class)
    val value get() = if (isResolved) _future.getCompletionExceptionOrNull() else null

    suspend fun waitPromise(): T = _future.await()
}

