package org.dweb_browser.helper

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.atomicfu.updateAndGet


class LateInit<T : Any>(initValue: T? = null) {
  val atomic = atomic(initValue)
  inline fun getOrInit(initFun: () -> T) = atomic.updateAndGet {
    it ?: initFun()
  }!!

  inline fun set(value: T) = atomic.update { value }
}
