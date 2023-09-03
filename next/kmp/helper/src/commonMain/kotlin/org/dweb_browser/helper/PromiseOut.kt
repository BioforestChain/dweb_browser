package org.dweb_browser.helper

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

open class PromiseOut<T> {
  companion object {
    fun <T : Any> resolve(value: T) = PromiseOut<T>().also { it.resolve(value) }
    fun <T : Any> reject(e: Throwable) = PromiseOut<T>().also { it.reject(e) }
  }

  private val _future = CompletableDeferred<T>()

  private val _lock = SynchronizedObject()

  open fun resolve(value: T) {
    synchronized(_lock) {
      _future.complete(value)
    }
  }

  open fun reject(e: Throwable) {
    synchronized(_lock) {
      _future.completeExceptionally(e)
    }
  }

  val isFinished get() = synchronized(_lock) { _future.isCompleted || _future.isCancelled }

  val isResolved get() = synchronized(_lock) { _future.isCompleted }

  val isRejected get() = synchronized(_lock) { _future.isCancelled }

  @OptIn(ExperimentalCoroutinesApi::class)
  var value
    get() = synchronized(_lock) { if (_future.isCompleted) _future.getCompleted() else null }
    set(value) {
      if (value != null) {
        resolve(value)
      }
    }

  suspend fun waitPromise(): T = _future.await()
}
