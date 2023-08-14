package org.dweb_browser.helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
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

  class TransformContext<K : Any, T>(val key: K, val value: T, var targetValue: T) {
    /**
     * 将该值标记成 true，将会让 setValue 取消赋值操作
     */
    var preventDefault = false
  }

  private val _observers = mutableListOf<Observer<K, *>>()

  /// 只读列表
  val observers get() = _observers.toList()

  class Observer<K : Any, T>(
    private val ob: Observable<K>,
    val key: K,
    var value: T,
    val valueClass: KClass<Any>,
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

  inline fun <reified T : Any> observe(key: K, initValue: T) =
    Observer(this, key, initValue, T::class as KClass<Any>)

  inline fun <reified T : Any> observe(
    key: K,
    initValue: T,
    noinline transform: (TransformContext<K, T>.() -> Unit)
  ) =
    Observer(this, key, initValue, T::class as KClass<Any>, transform)


  fun <T : Any> observeNullable(key: K, valueClass: KClass<T>, initValue: T? = null) =
    Observer(this, key, initValue, valueClass as KClass<Any>)

  fun <T : Any> observeNullable(
    key: K,
    valueClass: KClass<T>,
    initValue: T? = null,
    transform: (TransformContext<K, T?>.() -> Unit)
  ) =
    Observer(this, key, initValue, valueClass as KClass<Any>, transform)
}

