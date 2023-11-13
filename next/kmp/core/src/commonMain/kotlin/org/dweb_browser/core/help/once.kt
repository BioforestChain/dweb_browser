package org.dweb_browser.core.help

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate

inline fun <R> suspendOnce(crossinline runnable: suspend () -> R): suspend () -> R {
  val hasRun = atomic(false)
  var result: Any? = null
  return {
    hasRun.getAndUpdate { run ->
      if (!run) {
        result = runnable()
      }
      true
    }
    result as R
  }
}

inline fun <A, R> suspendOnce1(
  crossinline runnable: suspend (A) -> R
): suspend (A) -> R {
  val hasRun = atomic(false)
  var result: R? = null
  return {
    hasRun.getAndUpdate { run ->
      if (!run) {
        result = runnable(it)
      }
      true
    }
    result as R
  }
}