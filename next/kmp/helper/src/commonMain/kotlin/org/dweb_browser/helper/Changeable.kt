package org.dweb_browser.helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal class Changeable<T>(
  private val self: T, var context: CoroutineContext,
) {
  private val scope by lazy { CoroutineScope(context) }
  private val signal = lazy { Signal<T>() }
  private inline fun ifSignal(block: (Signal<T>) -> Unit) {
    if (signal.isInitialized()) {
      block(signal.value)
    }
  }

  val onChange by lazy { signal.value.toListener() }

  /**
   * 这里一定要判断size，因为 CoroutineScope 可能已经被 cancel 了，所以只要外部代码正确 onDispose 移除监听，那么这里就不会引发问题
   * 否则如果listener的size不为空，导致执行了runBlockingCatching带来异常，就说明代码有生命周期问题
   */
  fun emitChangeBackground(changes: T = self) = ifSignal { signal ->
    if (signal.size > 0) scope.launch {
      signal.emit(changes)
    }
  }

  suspend fun emitChange(changes: T = self) = ifSignal { signal ->
    signal.emit(changes)
  }

  fun clear() = ifSignal { signal ->
    signal.clear()
  }
}