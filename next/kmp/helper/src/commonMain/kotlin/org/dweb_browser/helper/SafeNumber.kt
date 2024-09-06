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
public class SafeInt(
  field: Int = 0,
) : SafeNumber<Int, SafeInt>(field) {


  override fun addInt(v: Int) {
    value += v
  }

  override fun addNumber(v: Int) {
    value += v
  }
}

public class SafeFloat(
  field: Float = 0f,
) : SafeNumber<Float, SafeFloat>(field) {

  override fun addInt(v: Int) {
    value += v
  }

  override fun addNumber(v: Float) {
    value += v
  }

}


@Suppress("UNCHECKED_CAST")
public sealed class SafeNumber<T : Number, Self : SafeNumber<T, Self>>(
  field: T,
) {
  protected val sync: SynchronizedObject = SynchronizedObject()

  public operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

  public operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T): Unit =
    synchronized(sync) {
      this.value = value
    }

  public var value: T = field
    protected set

  protected abstract fun addInt(v: Int)
  protected abstract fun addNumber(v: T)

  public operator fun inc(): Self = synchronized(sync) { addInt(1);this as Self }
  public operator fun dec(): Self = synchronized(sync) { addInt(-1);this as Self }

  public operator fun plusAssign(value: T) {
    synchronized(sync) { addNumber(value);this }
  }

  public operator fun minusAssign(value: T) {
    synchronized(sync) { addNumber(value);this }
  }

  override fun equals(other: Any?): Boolean = when (other) {
    is SafeNumber<*, *> -> other.value == value
    is Number -> other == value
    else -> false
  }

  override fun hashCode(): Int = value.hashCode()
  override fun toString(): String {
    return "SafeNumber($value)"
  }
}