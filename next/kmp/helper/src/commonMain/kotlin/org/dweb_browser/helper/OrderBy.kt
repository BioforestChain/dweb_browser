package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.CoroutineContext


val debugOrder = Debugger("order")

class OrderDeferred(var current: Job? = null) {
  val lock = SynchronizedObject()
  val keys = SafeLinkList<Any?>()
  fun <T> queue(scope: CoroutineScope, key: Any?, handler: suspend () -> T) = synchronized(lock) {
    val preJob = current;
    keys.add(key)
    val clearTimeout =
      debugOrder.timeout(scope, 1000, "queue@${hashCode()}") { "key=$key keys.size=${keys}" }
    scope.async(start = CoroutineStart.UNDISPATCHED) {
      preJob?.join();
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
//          WARNING("QAQ OrderDeferred maybe error: key=$key")
//        }
//      }
    }
  }

  fun <T> queue(context: CoroutineContext, key: Any?, handler: suspend () -> T) =
    queue(CoroutineScope(context), key, handler)

  suspend fun <T> queueAndAwait(key: Any?, handler: suspend () -> T) = coroutineScope {
    queue(this, key, handler).await()
  }
}

class OrderInvoker {
  private val queues = SafeHashMap<Int, OrderDeferred>()
  suspend fun <T> tryInvoke(param: Any?, key: Any? = param, invoker: suspend () -> T) = tryInvoke(
    order = when (param) {
      is OrderBy -> param.order
      else -> null
    },
    key = key,
    invoker,
  )

  suspend fun <T> tryInvoke(order: Int?, key: Any? = null, invoker: suspend () -> T) =
    when (order) {
      null -> invoker()
      else -> queues.getOrPut(order) { OrderDeferred() }.queueAndAwait(key = key, handler = invoker)
    }
}

interface OrderBy {
  val order: Int?
}