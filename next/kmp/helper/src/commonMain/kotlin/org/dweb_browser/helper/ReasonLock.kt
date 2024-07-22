package org.dweb_browser.helper

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ReasonLock {
  val locks = SafeHashMap<String, Mutex>()
  private val rootLock = Mutex()
  suspend fun lock(reasons: Collection<String>) = lock(*reasons.toTypedArray())
  suspend fun lock(vararg reasons: String): List<Mutex> = rootLock.withLock {
    // ToSet 去重
    reasons.toSet().map { reason -> locks.getOrPut(reason) { Mutex() } }
  }.onEach { mutex -> mutex.lock() }

  fun unlock(mutexList: List<Mutex>) {
    for (mutex in mutexList) {
      mutex.unlock()
    }
  }

  suspend inline fun <T> withLock(vararg reasons: String, block: () -> T): T {
    val mutexList = lock(*reasons)
    try {
      return block()
    } finally {
      unlock(mutexList)
    }
  }

  suspend inline fun <T> withLock(reason: String, block: () -> T) =
    locks.getOrPut(reason) { Mutex() }.withLock(action = block)
}