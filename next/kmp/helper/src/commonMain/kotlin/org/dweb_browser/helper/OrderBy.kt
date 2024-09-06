package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.CoroutineContext


public val debugOrder: Debugger = Debugger("order")

public class OrderDeferred(public var current: Job? = null) {
  public val lock: SynchronizedObject = SynchronizedObject()
  public val keys: SafeLinkList<Any?> = SafeLinkList()
  public fun <T> queue(scope: CoroutineScope, key: Any?, handler: suspend () -> T): Deferred<T> =
    synchronized(lock) {
      val preJob = current
      keys.add(key)
      val clearTimeout =
        debugOrder.timeout(scope, 1000, "queue@${hashCode()}") { "key=$key keys=${keys}" }
      scope.async(start = CoroutineStart.UNDISPATCHED) {
        preJob?.join()
        clearTimeout()
        handler()
      }.also { job ->
        if (job.isCompleted) {
          current = null
          keys.remove(key)
        } else {
          current = job
          job.invokeOnCompletion {
            keys.remove(key)
//          done = true
//          synchronized(lock) {
//            if (current === job) {
//              current = null
//            }
//          }
          }
        }
//      if (done) {
//        if (key == "close" || key == "trySend") {
//        } else if (key is String && (
//              //
//              key.startsWith("send=IpcResponse") ||
//                  //
//                  key.startsWith("send=IpcRequest") ||
//                  //
//                  key.contains("file://http.std.dweb/listen"))
//        ) {
//        } else {
//          WARNING("QWQ OrderDeferred maybe error: key=$key")
//        }
//      }
      }
    }

  public fun <T> queue(
    context: CoroutineContext,
    key: Any?,
    handler: suspend () -> T,
  ): Deferred<T> =
    queue(CoroutineScope(context), key, handler)

  public suspend fun <T> queueAndAwait(key: Any?, handler: suspend () -> T): T = coroutineScope {
    queue(this, key, handler).await()
  }
}

public class OrderInvoker {
  private val queues = SafeHashMap<Int, OrderDeferred>()
  public suspend fun <T> tryInvoke(param: Any?, key: Any? = param, invoker: suspend () -> T): T =
    tryInvoke(
      order = when (param) {
        is OrderBy -> param.order
        else -> null
      },
      key = key,
      invoker,
    )

  public suspend fun <T> tryInvoke(order: Int?, key: Any? = null, invoker: suspend () -> T): T =
    when (order) {
      null -> invoker()
      else -> queues.getOrPut(order) { OrderDeferred() }.queueAndAwait(key = key, handler = invoker)
    }
}

public interface OrderBy {
  public val order: Int?
}