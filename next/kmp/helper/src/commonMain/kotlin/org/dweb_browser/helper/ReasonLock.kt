package org.dweb_browser.helper

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public class ReasonLock {
  public val locks: SafeHashMap<String, Mutex> = SafeHashMap()
  public val rootLock: Mutex = Mutex()
  public suspend inline fun lock(reasons: Collection<String>): List<Mutex> =
    lock(*reasons.toTypedArray())

  public suspend inline fun lock(vararg reasons: String): List<Mutex> = rootLock.withLock {
    // ToSet 去重
    reasons.toSet().map { reason -> locks.getOrPut(reason) { Mutex() } }
  }.onEach { mutex -> mutex.lock() }

  public fun unlock(mutexList: List<Mutex>) {
    for (mutex in mutexList) {
      mutex.unlock()
    }
  }

  public suspend inline fun <T> withLock(vararg reasons: String, block: () -> T): T {
    val mutexList = lock(*reasons)
    try {
      return block()
    } finally {
      unlock(mutexList)
    }
  }

  public suspend inline fun <T> withLock(reason: String, block: () -> T): T =
    locks.getOrPut(reason) { Mutex() }.withLock(action = block)
}