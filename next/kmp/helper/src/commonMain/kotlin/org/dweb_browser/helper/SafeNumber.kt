package org.dweb_browser.helper

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.reflect.KProperty

/**
 * 对比 AtomicInt(),
 * AtomicInt 只能确保并发写入安全，但是对于读取安全并没有保证。比方说多线程同时进行两次 a++，AtomicInt 虽然能确保累加了 2，但是这个表达式可能返回同样的值
 *
 * SafeInt 就是在解决这个问题，两次 a++ 之间是有竞争的，确保累加 2 的同时，两次表达式一定会依次拿到想要的值
 * 事例用法为：
 *
 * var idAcc by SafeInt(0)
 *
 * val id = idAcc++
 * val id = ++idAcc
 */
class SafeInt(
  field: Int = 0,
) : SafeNumber<Int, SafeInt>(field) {


  override fun addInt(v: Int) {
    field += v
  }

  override fun addNumber(v: Int) {
    field += v
  }
}

class SafeFloat(
  field: Float = 0f,
) : SafeNumber<Float, SafeFloat>(field) {

  override fun addInt(v: Int) {
    field += v
  }

  override fun addNumber(v: Float) {
    field += v
  }

}


@Suppress("UNCHECKED_CAST")
sealed class SafeNumber<T : Number, Self : SafeNumber<T, Self>>(
  protected var field: T,
) {
  protected val sync: SynchronizedObject = SynchronizedObject()

  operator fun getValue(thisRef: Any?, property: KProperty<*>) = value

  operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = synchronized(sync) {
    this.field = value
  }

  val value get() = synchronized(sync) { field }

  protected abstract fun addInt(v: Int)
  protected abstract fun addNumber(v: T)

  operator fun inc() = synchronized(sync) { addInt(1);this as Self }
  operator fun dec() = synchronized(sync) { addInt(-1);this as Self }

  operator fun plusAssign(value: T) {
    synchronized(sync) { addNumber(value);this }
  }

  operator fun minusAssign(value: T) {
    synchronized(sync) { addNumber(value);this }
  }

  override fun equals(other: Any?): Boolean = when (other) {
    is SafeNumber<*, *> -> other.field == field
    is Number -> other == field
    else -> false
  }

  override fun hashCode() = value.hashCode()
  override fun toString(): String {
    return "SafeNumber($value)"
  }
}