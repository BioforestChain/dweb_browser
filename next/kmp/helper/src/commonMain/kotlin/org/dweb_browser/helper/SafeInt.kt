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
  private var field: Int = 0,
) {
  private val sync: SynchronizedObject = SynchronizedObject()

  operator fun getValue(thisRef: Any?, property: KProperty<*>) = value

  operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) = synchronized(sync) {
    this.field = value
  }

  val value get() = synchronized(sync) { field }

  operator fun inc() = synchronized(sync) { field++;this }
  operator fun dec() = synchronized(sync) { field--;this }
  override fun equals(other: Any?): Boolean = when (other) {
    is SafeInt -> other.field == field
    is Number -> other == field
    else -> false
  }

  override fun hashCode(): Int {
    return value
  }
}
