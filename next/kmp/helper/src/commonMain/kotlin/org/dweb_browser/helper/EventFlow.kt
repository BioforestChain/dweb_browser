package org.dweb_browser.helper

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

typealias Callback<T> = suspend (args: T) -> Unit
typealias SimpleCallback = suspend (Unit) -> Unit


/**
 *scope 生命周期
 * isAwaitEmit 是否等待listen 全部触发完成才返回emit
 * tip 标识，用于调试
 */
open class EventFlow<T>(
  val scope: CoroutineScope,
  val tip: String = ""
) {
  //用于存储和发送事件
  private val eventEmitter = MutableSharedFlow<T>(
    replay = 0,//相当于粘性数据
    extraBufferCapacity = 0,//接受的慢时候，发送的入栈
    onBufferOverflow = BufferOverflow.SUSPEND // 缓冲区溢出的时候挂起 背压
  ) // 热流，在emit 之后去监听不会触发该新注册的监听


  // 等待全部的监听触发
  private var eventCollect = SafeInt(0)

  // 等待listen全部触发完成
  private var awaitEmit = CompletableDeferred<Unit>()


  suspend fun emit(event: T) {
    eventEmitter.emit(event)
    if (eventCollect.value > 0) {
//      println("🍄 emit-start $tip ${eventCollect.value}")
      awaitEmit.await()
//      println("🍄 emit-end  $tip ${eventCollect.value}")
    }
  }

  // 监听数据
  fun listen(cb: Callback<T>) {
    eventCollect++
    scope.launch(start = CoroutineStart.UNDISPATCHED) {
      try {
        eventEmitter.collect {
          cb.invoke(it)
          if (eventCollect.value > 0) {
            eventCollect--
            if (eventCollect.value == 0) {
              awaitEmit.complete(Unit)
            }
          }
        }
      } catch (e: CancellationException) {
      }
    }

  }

  fun toListener() = Listener(this)
}

class SimpleEventFlow(
  scope: CoroutineScope,
  tip: String = ""
) : EventFlow<Unit>(scope, tip) {
  suspend fun emit() {
    this.emit(Unit)
  }

}

// 监听生成器
class Listener<Args>(private val eventFlow: EventFlow<Args>) {
  operator fun invoke(cb: Callback<Args>) = eventFlow.listen(cb)
}

typealias Remover = () -> Boolean

fun Remover.removeWhen(listener: Signal.Listener<*>) = listener {
  this@removeWhen()
}

fun Remover.removeWhen(lifecycleScope: CoroutineScope) = lifecycleScope.launch {
  CompletableDeferred<Unit>().await()
}.invokeOnCompletion {
  this@removeWhen()
}
