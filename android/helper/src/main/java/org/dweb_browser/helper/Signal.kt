package org.dweb_browser.helper

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.Collections.newSetFromMap
import java.util.concurrent.ConcurrentHashMap

typealias Callback<Args> = suspend (args: Args) -> Any?
typealias SimpleCallback = Callback<Unit>
typealias OffListener = (Unit) -> Boolean

/** 控制器 */
enum class SIGNAL_CTOR {
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
  private val listenerSet = mutableSetOf<Callback<Args>>();

  @Synchronized
  fun listen(cb: Callback<Args>): OffListener {
    // TODO emit 时的cbs 应该要同步进行修改？
    listenerSet.add(cb)
    return { off(cb) }
  }

  fun toFlow() = channelFlow {
    val off = listen {
      send(it)
    }
    awaitClose {
      off(Unit)
    }
  }

  class Listener<Args>(val signal: Signal<Args>) {
    operator fun invoke(cb: Callback<Args>) = signal.listen(cb)

    fun toFlow() = signal.toFlow()
  }

  fun toListener() = Listener(this)

  @Synchronized
  fun off(cb: Callback<Args>): Boolean {
    return listenerSet.remove(cb)
  }

  suspend fun emit(args: Args) {
    val cbs = synchronized(listenerSet) { listenerSet.toSet() }
    // 这里拷贝一份，避免中通对其读写的时候出问题
    _emit(args, cbs)
  }

  private suspend fun _emit(args: Args, cbs: Set<Callback<Args>>) {
    // 这里拷贝一份，避免中通对其读写的时候出问题
    for (cb in cbs) {
      try {
        when (cb(args)) {
          SIGNAL_CTOR.OFF -> off(cb)
          SIGNAL_CTOR.BREAK -> break
        }
      } catch (e: Throwable) {
        e.printStackTrace()
      }
    }
  }

  suspend fun emitAndClear(args: Args) {
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
}


class SimpleSignal : Signal<Unit>() {
  suspend fun emit() {
    emit(Unit)
  }

  suspend fun emitAndClear() {
    emitAndClear(Unit)
  }
};
