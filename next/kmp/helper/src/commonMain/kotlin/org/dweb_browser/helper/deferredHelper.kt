package org.dweb_browser.helper

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

/**
 * for alternative try-finally
 */
suspend fun <T> Deferred<T>.awaitResult() = runCatching { await() }

/**
 * 一个 Deferred，但是可以用来做回调监听
 */
class DeferredSignal<T>(val deferred: Deferred<T>) : Deferred<T> by deferred {
  @OptIn(ExperimentalCoroutinesApi::class)
  operator fun invoke(handler: (result: Result<T>) -> Unit) {
    // 如果还在等待中，尝试注册
    if (deferred.isActive) {
      // 注意这里不是原子性的，所以这里注册的回调可能失败
      invokeOnCompletion {
        runCallback(handler)
      }
    }
    // 因此这里不论 isActive，都尝试执行
    runCallback(handler)
  }

  @OptIn(ExperimentalCoroutinesApi::class, InternalCoroutinesApi::class)
  private fun runCallback(handler: (result: Result<T>) -> Unit) {
    if (deferred.isCompleted) {
      handler(Result.success(deferred.getCompleted()))
    } else if (deferred.isCompleted) {
      handler(Result.failure(deferred.getCancellationException()))
    }
  }
}