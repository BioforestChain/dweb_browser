package org.dweb_browser.helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty

public class Observable<K : Any> {
  public data class Change<K, V>(val key: K, val newValue: V, val oldValue: V)

  private var customCoroutineScope: CoroutineScope? = null
  public var coroutineScope: CoroutineScope
    get() = (if (customCoroutineScope?.isActive == true) customCoroutineScope else null)
      ?: globalDefaultScope
    set(value) {
      customCoroutineScope = value
    }

  public val changeSignal: Signal<Change<K, *>> = Signal()
  public val onChange: Signal.Listener<Change<K, *>> = changeSignal.toListener()

  public class TransformContext<K : Any, T : Any?>(
    public val key: K,
    public val value: T,
    public var targetValue: T,
  ) {
    /**
     * 将该值标记成 true，将会让 setValue 取消赋值操作
     */
    public var preventDefault: Boolean = false
  }

  private val _observers = mutableListOf<Observer<K, *>>()

  /// 只读列表
  public val observers: Map<K, Observer<K, *>> get() = _observers.associateBy { it.key }

  public class Observer<K : Any, T : Any?>(
    private val ob: Observable<K>,
    public val key: K,
    public var value: T,
    public val transform: (TransformContext<K, T>.() -> Unit)? = null,
  ) {
    init {
      ob._observers.add(this)
    }

    public operator fun setValue(thisRef: Any, property: KProperty<*>, newValue: T) {
      set(newValue)
    }

    public fun set(newValue: T) {
      val inputValue = if (transform != null) {
        val context = TransformContext(key, value, newValue)
        transform.invoke(context)
        if (context.preventDefault) {
          return
        }
        context.targetValue
      } else newValue

      // 先判断指针
      if (inputValue === value) {
        return
      }
      // 再判断值
      if (inputValue != value) {
        val oldValue = value
        value = newValue
        ob.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
          ob.changeSignal.emit(Change(key, newValue, oldValue))
        }
      }
    }

    public operator fun getValue(thisRef: Any, property: KProperty<*>): T = value
  }

  public fun <T : Any> observe(
    key: K,
    initValue: T,
    transform: (TransformContext<K, T>.() -> Unit)? = null,
  ): Observer<K, T> = Observer(this, key, initValue, transform)


  public fun <T : Any?> observeNullable(
    key: K,
    initValue: T,
    transform: (TransformContext<K, T>.() -> Unit)? = null,
  ): Observer<K, T> = Observer(this, key, initValue, transform)
}

