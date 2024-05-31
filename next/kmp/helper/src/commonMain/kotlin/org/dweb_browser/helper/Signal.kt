package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

typealias SignalCallback<Args> = suspend SignalController<Args>.(args: Args) -> Unit
typealias SignalSimpleCallback = suspend SignalController<Unit>.(Unit) -> Unit

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
  BREAK,
  ;
}

class OffListener<Args>(val origin: Signal<Args>, val cb: SignalCallback<Args>) {
  operator fun invoke() = synchronized(origin) { origin.off(cb) }

  /**
   * 触发自身的监听函数
   */
  suspend fun emitSelf(args: Args) = origin._emit(args, setOf(cb))
  fun removeWhen(listener: Signal.Listener<*>) = listener {
    this@OffListener()
  }

  fun removeWhen(job: Job) = job.invokeOnCompletion {
    this@OffListener()
  }

  fun removeWhen(lifecycleScope: CoroutineScope) = lifecycleScope.launch {
    CompletableDeferred<Unit>().await()
  }.invokeOnCompletion {
    this@OffListener()
  }
}

@Suppress("UNCHECKED_CAST")
open class Signal<Args>(autoStart: Boolean = true) : SynchronizedObject() {
  protected val listenerSet = ChangeableSet<SignalCallback<Args>>()
  private var emitCached: MutableList<Args>? = null

  init {
    if (!autoStart) {
      emitCached = mutableListOf()
    }
  }

  private fun consumeEmitCache() = synchronized(this) {
    if (emitCached != null) {
      // 这里拷贝一份，避免中通对其读写的时候出问题
      val argsList = emitCached!!.toList()
      emitCached = null
      val lockObject = this
      CoroutineScope(defaultAsyncExceptionHandler).launch {
        val cbs = synchronized(lockObject) { listenerSet.toSet() }
        for (args in argsList) {
          _emit(args, cbs)
        }
      }
    }
  }

  val size get() = listenerSet.size
  fun isNotEmpty() = listenerSet.isNotEmpty()

  open fun listen(cb: SignalCallback<Args>): OffListener<Args> = synchronized(this) {
    // TODO emit 时的cbs 应该要同步进行修改？
    listenerSet.add(cb)
    consumeEmitCache()
    return OffListener(this, cb)
  }

  private val whenNoEmptyCallbacks = lazy { mutableSetOf<() -> Unit>() }
  fun whenListenerSizeChange(cb: suspend () -> Unit) = listenerSet.onChange {
    cb()
  }

  fun whenNoEmpty(cb: () -> Unit) = ({
    if (listenerSet.isNotEmpty()) {
      cb()
    }
  }).let { wrappedCb ->
    wrappedCb()
    whenListenerSizeChange(wrappedCb)
  }

  fun whenEmpty(cb: () -> Unit) = ({
    if (listenerSet.isEmpty()) {
      cb()
    }
  }).let { wrappedCb ->
    wrappedCb()
    whenListenerSizeChange(wrappedCb)
  }

  internal fun off(cb: SignalCallback<Args>) = synchronized(this) { listenerSet.remove(cb) }


  /**
   * Child 采用独立的实现，从而避开 clear 的影响
   */
  data class Child<Args, F : Any, R>(
    val parentSignal: Signal<Args>,
    val childSignal: Signal<R>,
    val filter: (Args) -> F?,
    val map: (F) -> R,
  )

  private val children = mutableMapOf<Signal<*>, Child<Args, *, *>>()

  fun <F : Any, R> createChild(filter: (Args) -> F?, map: (F) -> R) =
    Child(this, Signal(), filter, map).also {
      synchronized(this) {
        children[it.childSignal] = it
      }
    }.childSignal

  fun removeChild(childSignal: Signal<*>) = synchronized(this) {
    children.remove(childSignal)?.let { true } ?: false
  }

  open suspend fun emit(args: Args) {
    if (emitCached != null) {
      synchronized(this) {
        emitCached!!.add(args)
        if (emitCached!!.size > 20) {
          printError("Signal", "too many emit cache args: ${emitCached!!.size}")
        }
      }
    } else {
      // 这里拷贝一份，避免中通对其读写的时候出问题
      val cbs = synchronized(this) {
        when {
          listenerSet.isEmpty() -> null
          else -> listenerSet.toSet()
        }
      }
      if (cbs != null || children.isNotEmpty()) {
        _emit(args, cbs?: setOf())
      }
    }
  }

  internal suspend fun _emit(args: Args, cbs: Set<SignalCallback<Args>>) {
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
    val childList = synchronized(this) {
      children.values.toList()
    }

    for (child in childList) {
      try {

        when (val f = child.filter(args)) {
          null -> {}
          else -> {
            val childArgs = (child.map as (Any) -> Any?)(f)
            (child.childSignal as Signal<Any?>).emit(childArgs);
          }
        }
      } catch (e: Throwable) {
        e.printStackTrace()
      }
    }
  }

  suspend fun emitAndClear(args: Args) {
    // 拷贝一份，然后立刻清理掉原来的
    val cbs = synchronized(this) {
      listenerSet.toSet().also {
        listenerSet.clear()
      }
    }
    this._emit(args, cbs)
  };

  fun clear() {
    synchronized(this) {
      listenerSet.clear()
      emitCached = null
    }
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
    operator fun invoke(cb: SignalCallback<Args>) = signal.listen(cb)
    fun <F : Any, R> createChild(
      filter: (Args) -> F?, map: (F) -> R,
    ) = signal.createChild(filter, map).toListener()

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

    suspend fun awaitOnce(): Args {
      val res = CompletableDeferred<Args>()
      signal.listen {
        res.complete(it)
        offListener()
      }
      return res.await()
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
