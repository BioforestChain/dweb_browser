package org.dweb_browser.helper

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ReasonLock {
  val locks = SafeHashMap<String, Mutex>()
  suspend inline fun <T> withLock(vararg reasons: String, block: () -> T): T {
    val mutexList = mutableListOf<Mutex>()
    for (reason in reasons) {
      val mutex = locks.getOrPut(reason) { Mutex() }
      mutex.lock()
      mutexList.add(mutex)
    }
    try {
      return block()
    } finally {
      for (mutex in mutexList) {
        mutex.unlock()
      }
    }
  }

  suspend inline fun <T> withLock(reason: String, block: () -> T) =
    locks.getOrPut(reason) { Mutex() }.withLock(action = block)
}