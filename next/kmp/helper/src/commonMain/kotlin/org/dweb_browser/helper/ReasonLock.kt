package org.dweb_browser.helper

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ReasonLock {
  val locks = SafeHashMap<String, Mutex>()
  suspend inline fun lock(reasons: Collection<String>) = lock(*reasons.toTypedArray())
  suspend inline fun lock(vararg reasons: String): List<Mutex> =
    mutableListOf<Mutex>().also { mutexList ->
      for (reason in reasons) {
        val mutex = locks.getOrPut(reason) { Mutex() }
        mutex.lock()
        mutexList.add(mutex)
      }
    }

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