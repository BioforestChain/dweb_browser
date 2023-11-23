package org.dweb_browser.helper

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

class SuspendOnce<R>(val runnable: suspend () -> R) {
  private val hasRun = atomic(false)
  private var result: Any? = null
  val haveRun get() = hasRun.value
  suspend operator fun invoke(): R {
    hasRun.update { run ->
      if (!run) {
        result = runnable()
      }
      true
    }
    return result as R
  }
}

class Once<R>(val runnable: () -> R) {
  private val hasRun = atomic(false)
  private var result: Any? = null
  val haveRun get() = hasRun.value
  operator fun invoke(): R {
    hasRun.update { run ->
      if (!run) {
        result = runnable()
      }
      true
    }
    return result as R
  }
}

inline fun <A, R> suspendOnce1(
  crossinline runnable: suspend (A) -> R
): suspend (A) -> R {
  val hasRun = atomic(false)
  var result: R? = null
  return {
    hasRun.update { run ->
      if (!run) {
        result = runnable(it)
      }
      true
    }
    result as R
  }
}