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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


public sealed class SuspendOnceBase<R> {
  public companion object {
    internal val noRun = CompletableDeferred<Nothing>().apply {
      completeExceptionally(
        Throwable("no run")
      )
    }
  }

  internal val lock = SynchronizedObject()
  internal var runned = noRun as Deferred<R>
  public val haveRun: Boolean get() = runned !== noRun
  public suspend fun getResult(): R = this.runned.await()
  public fun getResultOrNull(): R? = when {
    haveRun -> this.runned.getCompletedOrNull()
    else -> null
  }

  public fun reset(cause: Throwable? = null, doCancel: Boolean = true) {
    synchronized(lock) {
      if (doCancel && this.runned !== noRun) {
        this.runned.cancel("reset", cause)
      }
      this.runned = noRun
    }
  }

  internal suspend inline fun doInvoke(crossinline doRun: CoroutineScope.() -> Deferred<R>): R {
    return coroutineScope {
      synchronized(lock) {
        if (runned === noRun) {
          runned = doRun()
        }
        runned
      }
    }.await()
  }
}

public class SuspendOnce<R>(
  private val before: (suspend (SuspendOnce<R>.() -> Unit))? = null,
  public val runnable: suspend CoroutineScope.() -> R,
) : SuspendOnceBase<R>() {
  public suspend operator fun invoke(): R {
    before?.invoke(this)
    return doInvoke {
      async(SupervisorJob(), start = CoroutineStart.UNDISPATCHED) {
        runnable()
      }
    }
  }
}

public class SuspendOnce1<A1, R>(
  private val before: (suspend (SuspendOnce1<A1, R>.(A1) -> Unit))? = null,
  public val runnable: suspend CoroutineScope.(A1) -> R,
) : SuspendOnceBase<R>() {

  public suspend operator fun invoke(arg1: A1): R {
    before?.invoke(this, arg1)
    return doInvoke {
      async(start = CoroutineStart.UNDISPATCHED) {
        runnable(arg1)
      }
    }
  }
}

public sealed class OnceBase<R> {
  public companion object {
    internal val noRun = Result.failure<Nothing>(Throwable("no run"))
  }

  internal val lock = SynchronizedObject()
  internal var hasRun = false
  private var result: Result<R> = noRun
  public val haveRun: Boolean get() = hasRun

  public fun getResult(): R = result.getOrThrow()
  public fun reset() {
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
public class Once<R>(
  public val before: (Once<R>.() -> Unit)? = null,
  public val runnable: () -> R,
) : OnceBase<R>() {
  public operator fun invoke(): R {
    before?.invoke(this)
    return doInvoke { runnable() }
  }
}

/**
 * 这里的 before 参数用来做一些前置判断，一般用来检查状态、检查参数，然后抛出异常
 */
public class Once1<A1, R>(
  public val before: ((Once1<A1, R>.(A1) -> Unit))? = null,
  public val runnable: (A1) -> R,
) : OnceBase<R>() {

  public operator fun invoke(arg1: A1): R {
    before?.invoke(this, arg1)
    return doInvoke { runnable(arg1) }
  }
}

public class SuspendOnceWithKey(private val coroutineScope: CoroutineScope) {
  private val executedKeys = mutableSetOf<String>()
  private val mutex = Mutex()

  public suspend fun executeOnce(key: String, action: suspend () -> Unit): Unit = mutex.withLock {
    if (key !in executedKeys) {
      executedKeys.add(key)
      coroutineScope.launch {
        action()
        executedKeys.remove(key) // 增加移除操作
      }
    }
  }
}