package org.dweb_browser.helper

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

public typealias Callback<T> = suspend (args: T) -> Unit
public typealias SimpleCallback = suspend (Unit) -> Unit


/**
 *scope 生命周期
 * isAwaitEmit 是否等待listen 全部触发完成才返回emit
 * tip 标识，用于调试
 */
public open class EventFlow<T>(
  public val scope: CoroutineScope,
  public val tip: String = "",
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


  public suspend fun emit(event: T) {
    eventEmitter.emit(event)
    if (eventCollect.value > 0) {
//      println("🍄 emit-start $tip ${eventCollect.value}")
      awaitEmit.await()
//      println("🍄 emit-end  $tip ${eventCollect.value}")
    }
  }

  // 监听数据
  public fun listen(cb: Callback<T>) {
    eventCollect++
    scope.launch(start = CoroutineStart.UNDISPATCHED) {
      eventEmitter.collect {
        cb.invoke(it)
        if (eventCollect.value > 0) {
          eventCollect--
          if (eventCollect.value == 0) {
            awaitEmit.complete(Unit)
          }
        }
      }
    }

  }

  public fun toListener(): Listener<T> = Listener(this)
}

public class SimpleEventFlow(
  scope: CoroutineScope,
  tip: String = "",
) : EventFlow<Unit>(scope, tip) {
  public suspend fun emit() {
    this.emit(Unit)
  }

}

// 监听生成器
public class Listener<Args>(private val eventFlow: EventFlow<Args>) {
  public operator fun invoke(cb: Callback<Args>): Unit = eventFlow.listen(cb)
}

public typealias Remover = () -> Boolean

public fun Remover.removeWhen(listener: Signal.Listener<*>): OffListener<out Any?> = listener {
  this@removeWhen()
}

public fun Remover.removeWhen(lifecycleScope: CoroutineScope): DisposableHandle =
  lifecycleScope.launch {
    CompletableDeferred<Unit>().await()
  }.invokeOnCompletion {
    this@removeWhen()
  }
