package org.dweb_browser.helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

typealias Callback<Args> = suspend SignalController<Args>.(args: Args) -> Unit
typealias SimpleCallback = suspend SignalController<Unit>.(Unit) -> Unit
typealias OffListener = () -> Boolean

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


open class Signal<Args> {
  protected val listenerSet = mutableSetOf<Callback<Args>>();

  val size get() = listenerSet.size

  @Synchronized
  open fun listen(cb: Callback<Args>): OffListener {
    // TODO emit 时的cbs 应该要同步进行修改？
    listenerSet.add(cb)
    return { off(cb) }
  }

  @Synchronized
  private fun off(cb: Callback<Args>) = listenerSet.remove(cb)

  open suspend fun emit(args: Args) {
    // 这里拷贝一份，避免中通对其读写的时候出问题
    val cbs = synchronized(listenerSet) { listenerSet.toSet() }
    _emit(args, cbs)
  }

  protected suspend fun _emit(args: Args, cbs: Set<Callback<Args>>) {
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

    /**
     * 立即执行
     */
    suspend operator fun invoke(firstValue: Args, cb: Callback<Args>) = signal.listen(cb).also {
      signal._emit(firstValue, setOf(cb))
    }

    fun toFlow() = signal.toFlow()
  }

  fun toListener() = Listener(this)
}

/**
 * 有状态的监听器
 */
class StatefulSignal<Args>(var state: Args, private val context: CoroutineContext) :
  Signal<Args>() {
  override fun listen(cb: Callback<Args>) = super.listen(cb).also {
    /// 立即执行
    runBlockingCatching(context) {
      _emit(state, setOf(cb))
    }.getOrThrow()
  }


  override suspend fun emit(args: Args) {
    state = args
    super.emit(args)
  }

  fun emitBlocking(args: Args) {
    runBlockingCatching(context) {
      _emit(state, listenerSet)
    }.getOrThrow()
  }

  fun toWatcher() = toListener()
}


class SimpleSignal : Signal<Unit>() {
  suspend fun emit() {
    emit(Unit)
  }

  suspend fun emitAndClear() {
    emitAndClear(Unit)
  }

//  override fun listen(cb: SimpleCallback) = super.listen { _ -> cb.invoke(this) }
};

fun <T> OffListener.removeWhen(listener: Signal.Listener<T>) = listener {
  this@removeWhen()
}