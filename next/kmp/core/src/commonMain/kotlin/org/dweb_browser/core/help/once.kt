package org.dweb_browser.core.help

enum class ARG_COUNT {
  ONE
}

inline fun <A, R> suspendOnce(
  t: ARG_COUNT,
  crossinline runnable: suspend (A) -> R
): suspend (A) -> R {
  var runned = false
  var result: R? = null
  return {
    if (!runned) {
      runned = true
      result = runnable(it)
    }
    result as R
  }
}

inline fun <R> suspendOnce(crossinline runnable: suspend () -> R): suspend () -> R {
  var runned = false
  var result: Any? = null
  return {
    if (!runned) {
      runned = true
      result = runnable()
    }
    result as R
  }
}