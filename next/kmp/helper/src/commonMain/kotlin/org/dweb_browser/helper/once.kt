@file:Suppress("UNCHECKED_CAST")

package org.dweb_browser.helper

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class SuspendOnce<R>(val runnable: suspend CoroutineScope.() -> R) {
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
        result = coroutineScope {
          async {
            runnable()
          }
        }
      }
      true
    }
    return result.await()
  }
}


private val noRun = CompletableDeferred<Nothing>().apply {
  completeExceptionally(
    Throwable("no run")
  )
}

class SuspendOnce1<A1, R>(
  val before: (suspend ((A1) -> Unit))? = null,
  val runnable: suspend CoroutineScope.(A1) -> R,
) {
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
    before?.invoke(arg1)
    hasRun.update { run ->
      if (!run) {
        result = coroutineScope {
          async {
            runnable(arg1)
          }
        }
      }
      true
    }
    return result.await()
  }
}

class Once<R>(
  val before: ((() -> Unit))? = null,
  val runnable: () -> R
) {
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
    before?.invoke()
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

class Once1<A1, R>(
  val before: (((A1) -> Unit))? = null,
  val runnable: (A1) -> R
) {
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

  operator fun invoke(arg1: A1): R {
    before?.invoke(arg1)
    hasRun.update { run ->
      if (!run) {
        result = runnable(arg1)
      }
      true
    }
    return result as R
  }

  @Suppress("UNCHECKED_CAST")
  val cache get() = result as R?
}