package org.dweb_browser.core.help

import kotlinx.coroutines.launch
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.globalDefaultScope

open class AdapterManager<T> {
  private val adapterOrderMap = SafeHashMap<T, Float>()
  private var orderAdapters = listOf<T>()
  private val onChangeSignal = Signal<T>()
  private val onChangeLazy = lazy { onChangeSignal.toListener() }
  val onChange by onChangeLazy
  val adapters get() = orderAdapters

  /**
   * order越大，排名越靠前
   */
  open fun append(order: Float, adapter: T): () -> Boolean {
    adapterOrderMap[adapter] = order
    orderAdapters = adapterOrderMap.toList().sortedBy { (_, b) -> -b }.map { (adapter) -> adapter }
    if (onChangeLazy.isInitialized()) {
      globalDefaultScope.launch {
        onChangeSignal.emit(adapter)
      }
    }
    return { remove(adapter) }
  }

  open fun append(order: Int = 0, adapter: T) = append(order.toFloat(), adapter)

  open fun remove(adapter: T) = adapterOrderMap.remove(adapter) != null
}

class OffAdapter<T>(val manager: AdapterManager<T>, val adapter: T) {
  operator fun invoke() {
    manager.remove(adapter)
  }

  fun removeWhen(listener: Signal.Listener<*>) = listener {
    this@OffAdapter()
  }
}