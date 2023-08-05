package org.dweb_browser.helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ChangeableSet<E>(private val scope: CoroutineScope = GlobalScope) : LinkedHashSet<E>() {
  private val _changeSignal = Signal<ChangeableSet<E>>()
  val onChange = _changeSignal.toListener()
  fun emitChange() = scope.launch(ioAsyncExceptionHandler) {
    _changeSignal.emit(this@ChangeableSet)
  }

  override fun add(element: E): Boolean {
    return super.add(element).also { if (it) emitChange() }
  }

  override fun clear() {
    return super.clear().also { emitChange() }
  }

  override fun remove(element: E): Boolean {
    return super.remove(element).also { if (it) emitChange() }
  }

  override fun addAll(elements: Collection<E>): Boolean {
    return super.addAll(elements).also { if (it) emitChange() }
  }

  override fun removeAll(elements: Collection<E>): Boolean {
    return super.removeAll(elements).also { if (it) emitChange() }
  }

  override fun retainAll(elements: Collection<E>): Boolean {
    return super.retainAll(elements).also { if (it) emitChange() }
  }
}