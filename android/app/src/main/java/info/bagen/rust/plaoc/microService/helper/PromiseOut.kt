package info.bagen.rust.plaoc.microService.helper

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture

open class PromiseOut<T : Any> {
    companion object {
        fun <T : Any> resolve(value: T) = PromiseOut<T>().also { it.resolve(value) }
        fun <T : Any> reject(e: Throwable) = PromiseOut<T>().also { it.reject(e) }
    }

    private val _future = CompletableFuture<T>()


    open fun resolve(value: T) {
        _future.complete(value)
    }


    open fun reject(e: Throwable) {
        _future.get()

        _future.completeExceptionally(e)
    }

    val isFinished get() = _future.isDone || _future.isCompletedExceptionally
    val isResolved get() = _future.isDone
    val isRejected get() = _future.isCompletedExceptionally

    val value get() = if (isResolved) _future.get() else null

    suspend fun waitPromise(): T = _future.await()
}
