package org.dweb_browser.microservice.help

import org.dweb_browser.helper.Signal

open class AdapterManager<T> {
  private val adapterOrderMap = mutableMapOf<T, Int>()
  private var orderAdapters = listOf<T>()
  val adapters get() = orderAdapters
  fun append(order: Int = 0, adapter: T): () -> Boolean {
    adapterOrderMap[adapter] = order
    orderAdapters =
      adapterOrderMap.toList().sortedBy { (_, b) -> -b }.map { (adapter) -> adapter }
    return { remove(adapter) }
  }

  fun remove(adapter: T) = adapterOrderMap.remove(adapter) != null
}

class OffAdapter<T>(val manager: AdapterManager<T>, val adapter: T) {
  operator fun invoke() {
    manager.remove(adapter)
  }

  fun removeWhen(listener: Signal.Listener<*>) = listener {
    this@OffAdapter()
  }
}