package org.dweb_browser.helper

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

typealias Callback<Args> = suspend SignalController<Args>.(args: Args) -> Unit
typealias SimpleCallback = suspend SignalController<Unit>.(Unit) -> Unit

/** 控制器 */
class SignalController<Args>(val args: Args, val offListener: () -> Unit, val breakEmit: () -> Unit)
private enum class SIGNAL_CTOR {
  /**
   * 返回该值，会解除监听
   */
  OFF,

  /**
   * 返回该值，会让接下来的其它监听函数不再触发
   */
  BREAK, ;
}

class OffListener<Args>(val origin: Signal<Args>, val cb: Callback<Args>) {
  @Synchronized
  operator fun invoke() = origin.off(cb)

  /**
   * 触发自身的监听函数
   */
  suspend fun emitSelf(args: Args) = origin._emit(args, setOf(cb))
  fun removeWhen(listener: Signal.Listener<*>) = listener {
    this@OffListener()
  }
}

typealias Remover = () -> Boolean

fun <T> Remover.removeWhen(listener: Signal.Listener<T>) = listener {
  this@removeWhen()
}

@Suppress("UNCHECKED_CAST")
open class Signal<Args> {
  protected val listenerSet = mutableSetOf<Callback<Args>>();

  val size get() = listenerSet.size

  @Synchronized
  open fun listen(cb: Callback<Args>): OffListener<Args> {
    // TODO emit 时的cbs 应该要同步进行修改？
    listenerSet.add(cb)
    return OffListener(this, cb)
  }

  @Synchronized
  internal fun off(cb: Callback<Args>) = listenerSet.remove(cb)


  /**
   * Child 采用独立的实现，从而避开 clear 的影响
   */
  private data class Child<Args, R>(
    val parentSignal: Signal<Args>,
    val childSignal: Signal<R>,
    val filter: (Args) -> Boolean,
    val map: (Args) -> R
  )

  private val children = mutableMapOf<Signal<*>, Child<Args, *>>()
  fun <R> createChild(filter: (Args) -> Boolean, map: (Args) -> R) =
    Child(this, Signal(), filter, map).also {
      synchronized(children) {
        children[it.childSignal] = it
      }
    }.childSignal

  fun removeChild(childSignal: Signal<*>) = synchronized(children) {
    children.remove(childSignal)?.let { true } ?: false
  }

  open suspend fun emit(args: Args) {
    // 这里拷贝一份，避免中通对其读写的时候出问题
    val cbs = synchronized(listenerSet) { listenerSet.toSet() }
    _emit(args, cbs)
  }

  internal suspend fun _emit(args: Args, cbs: Set<Callback<Args>>) {
    var signal: SIGNAL_CTOR? = null
    val ctx = SignalController(args, { signal = SIGNAL_CTOR.OFF }, { signal = SIGNAL_CTOR.BREAK })
    for (cb in cbs) {
      try {
        cb.invoke(ctx, args)
        when (signal) {
          SIGNAL_CTOR.OFF -> off(cb)
          SIGNAL_CTOR.BREAK -> break
          else -> Unit
        }
        signal = null
      } catch (e: Throwable) {
        e.printStackTrace()
      }
    }

    /// 然后触发孩子
    val childList = synchronized(children) {
      children.values.toList()
    }

    for (child in childList) {
      try {
        if (child.filter(args)) {
          val childArgs = child.map(args)
          (child.childSignal as Signal<Any?>).emit(childArgs);
        }
      } catch (e: Throwable) {
        e.printStackTrace()
      }
    }
  }

  suspend fun emitAndClear(args: Args) {
    // 拷贝一份，然后立刻清理掉原来的
    val cbs = synchronized(listenerSet) {
      listenerSet.toSet().also {
        listenerSet.clear()
      }
    }
    this._emit(args, cbs)
  };

  @Synchronized
  fun clear() {
    this.listenerSet.clear()
  }

  fun toFlow() = channelFlow {
    val off = listen { it ->
      send(it)
    }
    awaitClose {
      off()
    }
  }

  class Listener<Args>(val signal: Signal<Args>) {
    operator fun invoke(cb: Callback<Args>) = signal.listen(cb)
    fun toFlow() = signal.toFlow()

    suspend fun <T> Flow<T>.toListener(): Listener<T> {
      val flow = this
      val signal = Signal<T>()
      coroutineScope {
        launch {
          flow.collect {
            signal.emit(it)
          }
        }
      }
      return signal.toListener()
    }
  }

  fun toListener() = Listener(this)
}

class SimpleSignal : Signal<Unit>() {
  suspend fun emit() {
    emit(Unit)
  }

  suspend fun emitAndClear() {
    emitAndClear(Unit)
  }

};
