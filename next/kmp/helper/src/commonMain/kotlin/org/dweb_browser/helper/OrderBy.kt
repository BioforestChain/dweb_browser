package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope


class OrderDeferred(var current: Job? = null) {
  val lock = SynchronizedObject()
  fun <T> queue(key: Any?, scope: CoroutineScope, handler: suspend () -> T) = synchronized(lock) {
    val preJob = current;
    scope.async(start = CoroutineStart.UNDISPATCHED) {
      preJob?.join();
      handler()
    }.also { job ->
      if (job.isCompleted) {
        current = null
      } else {
        current = job
        job.invokeOnCompletion {
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

  suspend fun <T> queue(key: Any?, handler: suspend () -> T) = coroutineScope {
    queue(key, this, handler)
  }
}

class OrderInvoker {
  private val queues = SafeHashMap<Int, OrderDeferred>()
  suspend fun <T> tryInvoke(param: Any?, invoker: suspend () -> T) = tryInvoke(
    order = when (param) {
      is OrderBy -> param.order
      else -> null
    },
    key = param,
    invoker,
  )

  suspend fun <T> tryInvoke(order: Int?, key: Any? = null, invoker: suspend () -> T) =
    when (order) {
      null -> invoker()
      else -> queues.getOrPut(order) { OrderDeferred() }.queue(key = key, handler = invoker).await()
    }
}

interface OrderBy {
  val order: Int?
}