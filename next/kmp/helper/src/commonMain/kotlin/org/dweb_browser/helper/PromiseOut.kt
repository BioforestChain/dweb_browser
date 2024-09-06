package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

public open class PromiseOut<T> : SynchronizedObject() {

  public companion object {
    private val defaultScope by lazy { CoroutineScope(defaultAsyncExceptionHandler + CoroutineName("PromiseOut")) }
    public fun <T : Any> resolve(value: T): PromiseOut<T> =
      PromiseOut<T>().also { it.resolve(value) }

    public fun <T : Any> reject(e: Throwable): PromiseOut<T> = PromiseOut<T>().also { it.reject(e) }
  }

  private val _future = CompletableDeferred<T>()

  public open fun resolve(value: T) {
    synchronized(this) {
      _future.complete(value)
    }
  }

  public open fun resolve(value: Deferred<T>, scope: CoroutineScope = defaultScope) {
    scope.launch {
      resolve(value.await())
    }
  }

  public open fun reject(e: Throwable) {
    synchronized(this) {
      _future.completeExceptionally(e)
    }
  }

  public val isFinished: Boolean get() = synchronized(this) { _future.isCompleted || _future.isCancelled }

  public val isResolved: Boolean get() = synchronized(this) { _future.isCompleted }

  public val isRejected: Boolean get() = synchronized(this) { _future.isCancelled }

  @OptIn(ExperimentalCoroutinesApi::class)
  public var value: T?
    get() = synchronized(this) { if (_future.isCompleted) _future.getCompleted() else null }
    set(value) {
      if (value != null) {
        resolve(value)
      }
    }

  public suspend fun waitPromise(): T = _future.await()

  public fun alsoLaunchIn(
    scope: CoroutineScope,
    block: suspend CoroutineScope.() -> T,
  ): PromiseOut<T> =
    this.also { launchIn(scope, block) }

  @Suppress("MemberVisibilityCanBePrivate")
  public fun launchIn(scope: CoroutineScope, block: suspend CoroutineScope.() -> T) {
    scope.launch {
      try {
        resolve(block())
      } catch (e: Throwable) {
        reject(e)
      }
    }
  }
}


public inline fun <T> CompletableDeferred<T>.launchIn(
  scope: CoroutineScope,
  crossinline block: suspend CoroutineScope.() -> T,
) {
  scope.launch {
    try {
      complete(block())
    } catch (e: Throwable) {
      completeExceptionally(e)
    }
  }
}

public inline fun <T> CompletableDeferred<T>.alsoLaunchIn(
  scope: CoroutineScope,
  crossinline block: suspend CoroutineScope.() -> T,
): CompletableDeferred<T> = also { launchIn(scope, block) }