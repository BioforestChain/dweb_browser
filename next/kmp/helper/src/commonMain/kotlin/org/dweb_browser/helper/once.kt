@file:Suppress("UNCHECKED_CAST")

package org.dweb_browser.helper

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.atomicfu.update
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope


internal val noRun = CompletableDeferred<Nothing>().apply {
  completeExceptionally(
    Throwable("no run")
  )
}

sealed class SuspendOnceBase<R> {
  internal val lock = SynchronizedObject()
  internal var hasRun = noRun as Deferred<R>
  val haveRun get() = hasRun !== noRun
  suspend fun getResult() = hasRun.await()
  fun reset() {
    synchronized(lock) {
      if (hasRun !== noRun && !hasRun.isCancelled) {
        hasRun.cancel()
      }
      hasRun = noRun
    }
  }

  internal suspend inline fun doInvoke(crossinline doRun: CoroutineScope.() -> Deferred<R>): R {
    return coroutineScope {
      synchronized(lock) {
        if (hasRun === noRun) {
          hasRun = doRun()
        }
      }
      hasRun
    }.await()
  }
}

class SuspendOnce<R>(val runnable: suspend CoroutineScope.() -> R) : SuspendOnceBase<R>() {
  suspend operator fun invoke(): R {
    return doInvoke {
      async(start = CoroutineStart.UNDISPATCHED) {
        runnable()
      }
    }
  }
}

class SuspendOnce1<A1, R>(
  val before: (suspend ((A1) -> Unit))? = null,
  val runnable: suspend CoroutineScope.(A1) -> R,
) : SuspendOnceBase<R>() {

  suspend operator fun invoke(arg1: A1): R {
    return doInvoke {
      async(start = CoroutineStart.UNDISPATCHED) {
        runnable(arg1)
      }
    }
  }
}

class Once<R>(
  val before: ((() -> Unit))? = null,
  val runnable: () -> R,
) {
  private val lock = SynchronizedObject()
  private val hasRun = atomic(false)
  private var result: Any? = null
  val haveRun get() = hasRun.value
  suspend fun getResult() = result as R
  fun reset() {
    synchronized(lock) {
      hasRun.update {
        if (it) {
          result = null
        }
        false
      }
    }
  }

  operator fun invoke(): R {
    before?.invoke()
    return synchronized(lock) {
      hasRun.update { run ->
        if (!run) {
          result = runnable()
        }
        true
      }
      result as R
    }
  }

  @Suppress("UNCHECKED_CAST")
  val cache get() = result as R?
}

class Once1<A1, R>(
  val before: (((A1) -> Unit))? = null,
  val runnable: (A1) -> R,
) {
  private val lock = SynchronizedObject()
  private val hasRun = atomic(false)
  private var result: Any? = null
  val haveRun get() = hasRun.value
  suspend fun getResult() = result as R
  fun reset() {
    synchronized(lock) {
      hasRun.update {
        if (it) {
          result = null
        }
        false
      }
    }
  }

  operator fun invoke(arg1: A1): R {
    before?.invoke(arg1)
    return synchronized(lock) {
      hasRun.update { run ->
        if (!run) {
          result = runnable(arg1)
        }
        true
      }
      result as R
    }
  }

  @Suppress("UNCHECKED_CAST")
  val cache get() = result as R?
}