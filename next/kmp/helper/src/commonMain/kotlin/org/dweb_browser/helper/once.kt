@file:Suppress("UNCHECKED_CAST")

package org.dweb_browser.helper

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlin.coroutines.coroutineContext

class SuspendOnce<R>(val runnable: suspend () -> R) {
  private val hasRun = atomic(false)
  private lateinit var result: Deferred<R>
  val haveRun get() = hasRun.value
  suspend operator fun invoke(): R {
    hasRun.update { run ->
      if (!run) {
        result = CoroutineScope(coroutineContext + commonAsyncExceptionHandler).async {
          runnable()
        }
      }
      true
    }
    return result.await()
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
  @Suppress("UNCHECKED_CAST")
  val cache get() = result as R?
}

class SuspendOnce1<A1, R>(val runnable: suspend (A1) -> R) {
  private val hasRun = atomic(false)
  private lateinit var result: Deferred<R>
  val haveRun get() = hasRun.value
  suspend operator fun invoke(arg1: A1): R {
    hasRun.update { run ->
      if (!run) {
        result = CoroutineScope(coroutineContext + commonAsyncExceptionHandler).async {
          runnable(arg1)
        }
      }
      true
    }
    return result.await()
  }
}