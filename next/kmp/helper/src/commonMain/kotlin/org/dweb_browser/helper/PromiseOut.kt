package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

open class PromiseOut<T> : SynchronizedObject() {
  companion object {
    fun <T : Any> resolve(value: T) = PromiseOut<T>().also { it.resolve(value) }
    fun <T : Any> reject(e: Throwable) = PromiseOut<T>().also { it.reject(e) }
  }

  private val _future = CompletableDeferred<T>()

  open fun resolve(value: T) {
    synchronized(this) {
      _future.complete(value)
    }
  }

  open fun reject(e: Throwable) {
    synchronized(this) {
      _future.completeExceptionally(e)
    }
  }

  val isFinished get() = synchronized(this) { _future.isCompleted || _future.isCancelled }

  val isResolved get() = synchronized(this) { _future.isCompleted }

  val isRejected get() = synchronized(this) { _future.isCancelled }

  @OptIn(ExperimentalCoroutinesApi::class)
  var value
    get() = synchronized(this) { if (_future.isCompleted) _future.getCompleted() else null }
    set(value) {
      if (value != null) {
        resolve(value)
      }
    }

  suspend fun waitPromise(): T = _future.await()

  fun alsoLaunchIn(scope: CoroutineScope, block: suspend CoroutineScope.() -> T) =
    this.also { launchIn(scope, block) }

  fun launchIn(scope: CoroutineScope, block: suspend CoroutineScope.() -> T) {
    scope.launch {
      try {
        resolve(block())
      } catch (e: Throwable) {
        reject(e)
      }
    }
  }
}
