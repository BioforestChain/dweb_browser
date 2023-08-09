package org.dweb_browser.helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture
import java.util.function.BiConsumer

open class PromiseOut<T> {
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

  var value
    get() = synchronized(_future) { if (_future.isDone) _future.get() else null }
    set(value) {
      if (value != null) {
        resolve(value)
      }
    }

  suspend fun waitPromise(): T = _future.await()
}
