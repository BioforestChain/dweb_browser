@file:Suppress("UNCHECKED_CAST")

package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope


sealed class SuspendOnceBase<R> {
  companion object {
    internal val noRun = CompletableDeferred<Nothing>().apply {
      completeExceptionally(
        Throwable("no run")
      )
    }
  }

  internal val lock = SynchronizedObject()
  internal var runned = noRun as Deferred<R>
  val haveRun get() = runned !== noRun
  suspend fun getResult() = this.runned.await()
  fun reset(cause: Throwable? = null, doCancel: Boolean = true) {
    synchronized(lock) {
      if (doCancel && this.runned !== noRun) {
        println("QAQ isCancelled=${this.runned.isCancelled} isActive=${this.runned.isActive} isCompleted=${this.runned.isCompleted}")
        this.runned.cancel("reset", cause)
      }
      this.runned = noRun
    }
  }

  internal suspend inline fun doInvoke(crossinline doRun: CoroutineScope.() -> Deferred<R>): R {
    return coroutineScope {
      synchronized(lock) {
        if (this@SuspendOnceBase.runned === noRun) {
          this@SuspendOnceBase.runned = doRun()
        }
      }
      this@SuspendOnceBase.runned
    }.await()
  }
}

class SuspendOnce<R>(val runnable: suspend CoroutineScope.() -> R) : SuspendOnceBase<R>() {
  suspend operator fun invoke(): R {
    return doInvoke {
      async(SupervisorJob(), start = CoroutineStart.UNDISPATCHED) {
        runnable()
      }
    }
  }
}

class SuspendOnce1<A1, R>(
  private val before: (suspend (SuspendOnce1<A1, R>.(A1) -> Unit))? = null,
  val runnable: suspend CoroutineScope.(A1) -> R,
) : SuspendOnceBase<R>() {

  suspend operator fun invoke(arg1: A1): R {
    before?.invoke(this, arg1)
    return doInvoke {
      async(start = CoroutineStart.UNDISPATCHED) {
        runnable(arg1)
      }
    }
  }
}

sealed class OnceBase<R> {
  companion object {
    internal val noRun = Result.failure<Nothing>(Throwable("no run"))
  }

  internal val lock = SynchronizedObject()
  internal var hasRun = false
  private var result: Result<R> = noRun
  val haveRun get() = hasRun
  suspend fun getResult() = result as R
  fun reset() {
    synchronized(lock) {
      result = noRun
      hasRun = false
    }
  }

  internal inline fun doInvoke(crossinline doRun: () -> R): R {
    synchronized(lock) {
      if (!hasRun) {
        hasRun = true
        result = runCatching { doRun() }
      }
    }
    return result.getOrThrow()
  }
}

/**
 * 这里的 before 参数用来做一些前置判断，一般用来检查状态、检查参数，然后抛出异常
 */
class Once<R>(
  val before: (Once<R>.() -> Unit)? = null,
  val runnable: () -> R,
) : OnceBase<R>() {
  operator fun invoke(): R {
    before?.invoke(this)
    return doInvoke { runnable() }
  }
}

/**
 * 这里的 before 参数用来做一些前置判断，一般用来检查状态、检查参数，然后抛出异常
 */
class Once1<A1, R>(
  val before: ((Once1<A1, R>.(A1) -> Unit))? = null,
  val runnable: (A1) -> R,
) : OnceBase<R>() {

  operator fun invoke(arg1: A1): R {
    before?.invoke(this, arg1)
    return doInvoke { runnable(arg1) }
  }
}