package org.dweb_browser.core.help

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.defaultAsyncExceptionHandler

open class AdapterManager<T> {
  private val adapterOrderMap = SafeHashMap<T, Int>()
  private var orderAdapters = listOf<T>()
  private val onChangeSignal = Signal<T>()
  private val onChangeLazy = lazy { onChangeSignal.toListener() }
  val onChange by onChangeLazy
  private val scope by lazy { CoroutineScope(defaultAsyncExceptionHandler) }
  val adapters get() = orderAdapters
  open fun append(order: Int = 0, adapter: T): () -> Boolean {
    adapterOrderMap[adapter] = order
    orderAdapters = adapterOrderMap.toList().sortedBy { (_, b) -> -b }.map { (adapter) -> adapter }
    if (onChangeLazy.isInitialized()) {
      scope.launch {
        onChangeSignal.emit(adapter)
      }
    }
    return { remove(adapter) }
  }

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