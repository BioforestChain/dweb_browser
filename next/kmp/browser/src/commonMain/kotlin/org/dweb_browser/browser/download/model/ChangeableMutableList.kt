package org.dweb_browser.browser.download.model

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.ioAsyncExceptionHandler
import kotlin.coroutines.CoroutineContext

class ChangeableMutableList<E>(
  val cList: MutableList<E> = mutableStateListOf(),
  private val context: CoroutineContext = ioAsyncExceptionHandler
) {

  fun add(index: Int, element: E) {
    cList.add(index, element)
    emitBackground(ChangeableType.Add, element)
  }

  fun add(element: E) {
    cList.add(element)
    emitBackground(ChangeableType.Add, element)
  }

  fun addAll(elements: Collection<E>) {
    cList.addAll(elements)
    emitBackground(ChangeableType.PutAll)
  }

  fun remove(element: E): Boolean {
    return cList.remove(element).also {
      if (it) {
        emitBackground(ChangeableType.Remove, element)
      }
    }
  }

  fun removeAt(index: Int): E {
    val element = cList.removeAt(index)
    emitBackground(ChangeableType.Remove, element)
    return element
  }

  fun removeAt(element: E): Boolean {
    return cList.removeAll { element == it }.also {
      if (it) {
        emitBackground(ChangeableType.Remove, element)
      }
    }
  }

  fun clear() {
    cList.clear()
    emitBackground(ChangeableType.Clear)
  }

  fun containsKey(element: E) = cList.contains(element)

  fun isEmpty() = cList.isEmpty()

  fun get(index: Int) = cList.get(index)

  suspend fun suspendForEach(each: suspend (E) -> Unit) = cList.forEach { element -> each(element) }

  fun forEach(each: (E) -> Unit) = cList.forEach { element -> each(element) }

  private fun emitBackground(type: ChangeableType, element: E? = null) {
    CoroutineScope(context).launch {
      changeSignal.emit(Pair(type, element))
    }
  }

  private val changeSignal: Signal<Pair<ChangeableType, E?>> = Signal()
  val onChange = changeSignal.toListener()
}