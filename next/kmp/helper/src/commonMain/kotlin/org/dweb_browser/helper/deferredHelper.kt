package org.dweb_browser.helper

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job

/**
 * for alternative try-finally
 */
public suspend fun <T> Deferred<T>.awaitResult(): Result<T> = runCatching { await() }

@OptIn(ExperimentalCoroutinesApi::class)
public fun <T> Deferred<T>.getCompletedOrNull(): T? = when {
  isCompleted -> getCompleted()
  else -> null
}

/**
 * 一个 Deferred，但是可以用来做回调监听
 */
public class DeferredSignal<T>(public val deferred: Deferred<T>) : Deferred<T> by deferred {
  @OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
  public inline operator fun invoke(crossinline handler: (result: Result<T>) -> Unit): DisposableHandle =
    invokeOnCompletion(onCancelling = true, invokeImmediately = true) {
      if (deferred.isCompleted) {
        handler(Result.success(deferred.getCompleted()))
      } else if (deferred.isCancelled) {
        handler(Result.failure(deferred.getCancellationException()))
      }
    }

}

public suspend inline fun Job.await() {
  val deferred = CompletableDeferred<Unit>(SupervisorJob())
  // job.join()
  invokeOnCompletion {
    val error = when (it) {
      is CancellationException -> null//it.cause
      is Throwable -> it
      else -> null
    }
    when (error) {
      null -> deferred.complete(Unit)
      else -> deferred.completeExceptionally(error)
    }
  }
  deferred.await()
}

public fun CompletableJob.cancelOrThrow(cause: Throwable? = null) {
  when (cause) {
    is CancellationException -> cancel(cause)
    null -> cancel()
    else -> completeExceptionally(cause)
  }
}

public fun CoroutineScope.cancelOrThrow(cause: Throwable? = null) {
  when (cause) {
    is CancellationException -> cancel(cause)
    null -> cancel()
    else -> when (val job = coroutineContext.job) {
      is CompletableJob -> job.completeExceptionally(cause)
      else -> cancel()
    }
  }
}