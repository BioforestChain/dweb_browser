package org.dweb_browser.helper

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class LateInit<T : Any>(private var value: T? = null) {
  private val lock = Mutex()
  fun getOrInitSync(initFun: () -> T): T {
    while (value == null) {
      if (lock.tryLock()) {
        val result = value ?: initFun().also { value = it }
        lock.unlock()
        return result
      }
    }
    return value as T
  }

  suspend fun getOrInit(initFun: suspend () -> T) = lock.withLock {
    value ?: initFun().also { value = it }
  }

  fun set(value: T) {
    this.value = value
  }
}
