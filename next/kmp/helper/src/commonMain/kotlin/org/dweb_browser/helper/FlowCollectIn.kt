package org.dweb_browser.helper

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch

fun <T> Flow<T>.collectIn(scope: CoroutineScope, collector: FlowCollector<T>) =
  scope.launch { collect(collector) }

suspend inline fun <T> Flow<T>.collectIn(collector: FlowCollector<T>) =
  coroutineScope { collectIn(this, collector) }


/**
 * 继承 emit 所在作用域，执行 FlowCollector
 * 与常见的 Event.listen 这种模式类似
 */
fun <T> Flow<T>.listenAsync(collector: FlowCollector<T>) = collectIn(emptyScope, collector)

/**
 * 继承 emit 所在作用域，执行 FlowCollector
 * 与常见的 Event.listen 这种模式类似
 */
suspend fun <T> SharedFlow<T>.listen(collector: FlowCollector<T>) {
  val def = CompletableDeferred<Unit>()
  onSubscription { def.complete(Unit) }.listenAsync(collector)
  def.await()
}


class EventEmitter<T>(
  private val sharedFlow: MutableSharedFlow<T> = MutableSharedFlow()
) : MutableSharedFlow<T> by sharedFlow {
  private val afterSub = CompletableDeferred<Unit>()

  init {
    onSubscription {
      println("afterSub")
      afterSub.complete(Unit)
    }
  }

  override suspend fun emit(value: T) {
    afterSub.await()
    sharedFlow.emit(value)
  }

  override fun tryEmit(value: T): Boolean {
    if (afterSub.isActive) return false
    return sharedFlow.tryEmit(value)
  }
}