package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch


class OrderDeferred(var current: Job = CompletableDeferred(Unit)) {
  val lock = SynchronizedObject()
  fun queue(scope: CoroutineScope, handler: suspend () -> Unit) = synchronized(lock) {
    val preJob = current;
    current = scope.launch {
      preJob.join()
      handler()
    }
    current
  }

  suspend fun queue(handler: suspend () -> Unit) =
    queue(CoroutineScope(currentCoroutineContext()), handler)
}

class OrderInvoker {
  private val queues = SafeHashMap<Int, OrderDeferred>()
  suspend fun tryInvoke(param: Any?, invoker: suspend () -> Unit) {
    tryInvoke(
      when (param) {
        is OrderBy -> param.orderBy
        else -> null
      },
      invoker,
    )
  }

  suspend fun tryInvoke(orderBy: Int?, invoker: suspend () -> Unit) {
    if (orderBy == null) {
      invoker()
    } else {
      queues.getOrPut(orderBy) { OrderDeferred() }.queue(handler = invoker).join()
    }
  }
}

interface OrderBy {
  val orderBy: Int?
}