package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

public typealias SignalCallback<Args> = suspend SignalController<Args>.(args: Args) -> Unit
public typealias SignalSimpleCallback = suspend SignalController<Unit>.(Unit) -> Unit

/** 控制器 */
public class SignalController<Args>(
  public val args: Args,
  public val offListener: () -> Unit,
  public val breakEmit: () -> Unit,
)

private enum class SignalPolicy {
  /**
   * 返回该值，会解除监听
   */
  Off,

  /**
   * 返回该值，会让接下来的其它监听函数不再触发
   */
  Break,
  ;
}

public class OffListener<Args>(
  public val origin: Signal<Args>,
  public val cb: SignalCallback<Args>,
) {
  public operator fun invoke(): Boolean = synchronized(origin) { origin.off(cb) }

  /**
   * 触发自身的监听函数
   */
  public suspend fun emitSelf(args: Args): Unit = origin.emit(args, setOf(cb))
  public fun removeWhen(listener: Signal.Listener<*>): OffListener<out Any?> = listener {
    this@OffListener()
  }

  public fun removeWhen(job: Job): DisposableHandle = job.invokeOnCompletion {
    this@OffListener()
  }

  public fun removeWhen(lifecycleScope: CoroutineScope): DisposableHandle = lifecycleScope.launch {
    CompletableDeferred<Unit>().await()
  }.invokeOnCompletion {
    this@OffListener()
  }
}

@Suppress("UNCHECKED_CAST")
public open class Signal<Args>(autoStart: Boolean = true) : SynchronizedObject() {
  private val listenerSet: ChangeableSet<suspend SignalController<Args>.(args: Args) -> Unit> =
    ChangeableSet()
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
          emit(args, cbs)
        }
      }
    }
  }

  public val size: Int get() = listenerSet.size
  public fun isNotEmpty(): Boolean = listenerSet.isNotEmpty()

  public open fun listen(cb: SignalCallback<Args>): OffListener<Args> = synchronized(this) {
    // TODO emit 时的cbs 应该要同步进行修改？
    listenerSet.add(cb)
    consumeEmitCache()
    return OffListener(this, cb)
  }

  private val whenNoEmptyCallbacks = lazy { mutableSetOf<() -> Unit>() }

  @Suppress("MemberVisibilityCanBePrivate")
  public fun whenListenerSizeChange(cb: suspend () -> Unit): OffListener<ChangeableSet<suspend SignalController<Args>.(args: Args) -> Unit>> =
    listenerSet.onChange {
      cb()
    }

  public fun whenNoEmpty(cb: () -> Unit): OffListener<ChangeableSet<suspend SignalController<Args>.(args: Args) -> Unit>> =
    ({
      if (listenerSet.isNotEmpty()) {
        cb()
      }
    }).let { wrappedCb ->
      wrappedCb()
      whenListenerSizeChange(wrappedCb)
    }

  public fun whenEmpty(cb: () -> Unit): OffListener<ChangeableSet<suspend SignalController<Args>.(args: Args) -> Unit>> =
    ({
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
  public data class Child<Args, F : Any, R>(
    val parentSignal: Signal<Args>,
    val childSignal: Signal<R>,
    val filter: (Args) -> F?,
    val map: (F) -> R,
  )

  private val children = mutableMapOf<Signal<*>, Child<Args, *, *>>()

  public fun <F : Any, R> createChild(filter: (Args) -> F?, map: (F) -> R): Signal<R> =
    Child(this, Signal(), filter, map).also {
      synchronized(this) {
        children[it.childSignal] = it
      }
    }.childSignal

  public fun removeChild(childSignal: Signal<*>): Boolean = synchronized(this) {
    children.remove(childSignal)?.let { true } ?: false
  }

  public open suspend fun emit(args: Args) {
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
        emit(args, cbs ?: setOf())
      }
    }
  }

  internal suspend fun emit(args: Args, cbs: Set<SignalCallback<Args>>) {
    var signal: SignalPolicy? = null
    val ctx = SignalController(args, { signal = SignalPolicy.Off }, { signal = SignalPolicy.Break })
    for (cb in cbs) {
      try {
        cb.invoke(ctx, args)
        when (signal) {
          SignalPolicy.Off -> off(cb)
          SignalPolicy.Break -> break
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
            (child.childSignal as Signal<Any?>).emit(childArgs)
          }
        }
      } catch (e: Throwable) {
        e.printStackTrace()
      }
    }
  }

  public suspend fun emitAndClear(args: Args) {
    // 拷贝一份，然后立刻清理掉原来的
    val cbs = synchronized(this) {
      listenerSet.toSet().also {
        listenerSet.clear()
      }
    }
    this.emit(args, cbs)
  }

  public fun clear() {
    synchronized(this) {
      listenerSet.clear()
      emitCached = null
    }
  }

  public fun toFlow(): Flow<Args> = channelFlow {
    val off = listen {
      send(it)
    }
    awaitClose {
      off()
    }
  }

  public class Listener<Args>(public val signal: Signal<Args>) {
    public operator fun invoke(cb: SignalCallback<Args>): OffListener<Args> = signal.listen(cb)
    public fun <F : Any, R> createChild(
      filter: (Args) -> F?, map: (F) -> R,
    ): Listener<R> = signal.createChild(filter, map).toListener()

    public fun toFlow(): Flow<Args> = signal.toFlow()

    public suspend fun <T> Flow<T>.toListener(): Listener<T> {
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

    public suspend fun awaitOnce(): Args {
      val res = CompletableDeferred<Args>()
      signal.listen {
        res.complete(it)
        offListener()
      }
      return res.await()
    }
  }

  public fun toListener(): Listener<Args> = Listener(this)
}

public class SimpleSignal : Signal<Unit>() {
  public suspend fun emit() {
    emit(Unit)
  }

  public suspend fun emitAndClear() {
    emitAndClear(Unit)
  }

}
