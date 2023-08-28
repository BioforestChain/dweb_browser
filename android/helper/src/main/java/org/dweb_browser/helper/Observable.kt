package org.dweb_browser.helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty

@Suppress("UNCHECKED_CAST")
class Observable<K : Any> {
  data class Change<K, V>(val key: K, val newValue: V, val oldValue: V)

  val defaultCoroutineScope = CoroutineScope(ioAsyncExceptionHandler)
  private var customCoroutineScope: CoroutineScope? = null
  var coroutineScope
    get() = (if (customCoroutineScope?.isActive == true) customCoroutineScope else null)
      ?: defaultCoroutineScope
    set(value) {
      customCoroutineScope = value
    }

  val changeSignal = Signal<Change<K, *>>();
  val onChange = changeSignal.toListener()

  class TransformContext<K : Any, T : Any?>(val key: K, val value: T, var targetValue: T) {
    /**
     * 将该值标记成 true，将会让 setValue 取消赋值操作
     */
    var preventDefault = false
  }

  private val _observers = mutableListOf<Observer<K, *>>()

  /// 只读列表
  val observers get() = _observers.associateBy { it.key }

  class Observer<K : Any, T : Any?>(
    private val ob: Observable<K>,
    val key: K,
    var value: T,
    val transform: (TransformContext<K, T>.() -> Unit)? = null,
  ) {
    init {
      ob._observers.add(this)
    }

    operator fun setValue(thisRef: Any, property: KProperty<*>, newValue: T) {
      set(newValue)
    }

    fun set(newValue: T) {
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
        ob.coroutineScope.launch {
          ob.changeSignal.emit(Change(key, newValue, oldValue))
        }
      }
    }

    operator fun getValue(thisRef: Any, property: KProperty<*>) = value
  }

  fun <T : Any> observe(
    key: K,
    initValue: T,
    transform: (TransformContext<K, T>.() -> Unit)? = null
  ) = Observer(this, key, initValue, transform)


  fun <T : Any?> observeNullable(
    key: K,
    initValue: T,
    transform: (TransformContext<K, T>.() -> Unit)? = null
  ) = Observer(this, key, initValue, transform)
}

