package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch


class OrderDeferred(var current: Job? = null) {
  val lock = SynchronizedObject()
  fun queue(key: Any?, scope: CoroutineScope, handler: suspend () -> Unit) = synchronized(lock) {
    val preJob = current;
    scope.launch {
      preJob?.join();
      handler()
    }.also { job ->
      current = job
//      job.invokeOnCompletion {
//        synchronized(lock) {
//          if (current === job) {
//            current = null
//          }
//        }
//      }
      println("QAQ OrderDeferred queue: key=$key pre=$preJob cur=$current")
    }
  }

  suspend fun queue(key: Any?, handler: suspend () -> Unit) = coroutineScope {
    queue(key, this, handler)
  }

  suspend inline fun withLock(noinline block: suspend () -> Unit) {
    queue(null, handler = block)
  }
}

class OrderInvoker {
  private val queues = SafeHashMap<Int, OrderDeferred>()
  suspend fun tryInvoke(param: Any?, invoker: suspend () -> Unit) {
    tryInvoke(
      order = when (param) {
        is OrderBy -> param.order
        else -> null
      },
      key = param,
      invoker,
    )
  }

  suspend fun tryInvoke(order: Int?, key: Any? = null, invoker: suspend () -> Unit) {
    if (order == null) {
      invoker()
    } else {
      queues.getOrPut(order) { OrderDeferred() }.queue(key = key, handler = invoker).join()
    }
  }
}

interface OrderBy {
  val order: Int?
}