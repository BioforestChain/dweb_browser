@file:Suppress("UNCHECKED_CAST")

package org.dweb_browser.helper

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlin.coroutines.coroutineContext

class SuspendOnce<R>(val runnable: suspend () -> R) {
  private val hasRun = atomic(false)
  private var result: Deferred<R> = noRun
  val haveRun get() = hasRun.value
  suspend fun getResult() = result.await()
  fun reset() {
    hasRun.update {
      if (it) {
        result = noRun
      }
      false
    }
  }

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
  suspend fun getResult() = result as R
  fun reset() {
    hasRun.update {
      if (it) {
        result = null
      }
      false
    }
  }

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

private val noRun = CompletableDeferred<Nothing>().apply {
  completeExceptionally(
    Throwable("no run")
  )
}

class SuspendOnce1<A1, R>(val runnable: suspend (A1) -> R) {
  private val hasRun = atomic(false)
  private var result: Deferred<R> = noRun
  val haveRun get() = hasRun.value
  suspend fun getResult() = result.await()
  fun reset() {
    hasRun.update {
      if (it) {
        result = noRun
      }
      false
    }
  }

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