package org.dweb_browser.helper

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 截流锁，确保同一时间内的多次调用只会执行一次
 */
class ThrottleLocks<K, V> {
  val tasks = mutableMapOf<K, CompletableDeferred<V>>()
  val lock = Mutex()
  suspend inline fun getOrPut(key: K, runner: () -> Deferred<V>): V {
    var isPut = false
    val task = lock.withLock {
      tasks.getOrPut(key) {
        isPut = true
        CompletableDeferred()
      }
    }

    if (isPut) {
      try {
        task.complete(runner().await())
      } catch (e: Throwable) {
        task.completeExceptionally(e)
      }
      lock.withLock { tasks.remove(key, task) }
    }
    return task.await()
  }

  suspend fun getOrRun(key: K, runner: suspend CoroutineScope.() -> V): V {
    return getOrPut(key) { coroutineScope { async(block = runner) } }
  }
}