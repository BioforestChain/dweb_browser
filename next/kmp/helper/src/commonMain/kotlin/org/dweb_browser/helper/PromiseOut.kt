package org.dweb_browser.helper

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized
import kotlinx.coroutines.sync.Mutex

open class PromiseOut<T> {
  companion object {
    fun <T : Any> resolve(value: T) = PromiseOut<T>().also { it.resolve(value) }
    fun <T : Any> reject(e: Throwable) = PromiseOut<T>().also { it.reject(e) }
  }

  private val _future = CompletableDeferred<T>()
  @OptIn(InternalCoroutinesApi::class)
  private val _lock = SynchronizedObject()

  @OptIn(InternalCoroutinesApi::class)
  open fun resolve(value: T) {
    synchronized(_lock) {
      _future.complete(value)
    }
  }

  @OptIn(InternalCoroutinesApi::class)
  open fun reject(e: Throwable) {
    synchronized(_lock) {
      _future.completeExceptionally(e)
    }
  }

  @OptIn(InternalCoroutinesApi::class)
  val isFinished get() = synchronized(_lock) { _future.isCompleted || _future.isCancelled }
  @OptIn(InternalCoroutinesApi::class)
  val isResolved get() = synchronized(_lock) { _future.isCompleted }
  @OptIn(InternalCoroutinesApi::class)
  val isRejected get() = synchronized(_lock) { _future.isCancelled }

  @OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
  var value
    get() = synchronized(_lock) { if (_future.isCompleted) _future.getCompleted() else null }
    set(value) {
      if (value != null) {
        resolve(value)
      }
    }

  suspend fun waitPromise(): T = _future.await()
}
