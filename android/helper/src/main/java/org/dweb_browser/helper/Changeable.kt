package org.dweb_browser.helper

import kotlinx.coroutines.isActive
import kotlin.coroutines.CoroutineContext

class Changeable<T>(
  private val self: T, private val context: CoroutineContext
) {
  val signal = Signal<T>()
  val onChange = signal.toListener()

  class Watcher<T>(
    private val listener: Signal.Listener<T>,
    private val args: T,
    private val context: CoroutineContext
  ) {
    operator fun invoke(cb: Callback<T>) = runBlockingCatching(context) {
      listener(args, cb)
    }.getOrThrow()
  }

  val watch = Watcher(onChange, self, context)


  /**
   * 这里一定要判断size，因为 CoroutineScope 可能已经被 cancel 了，所以只要外部代码正确 onDispose 移除监听，那么这里就不会引发问题
   * 否则如果listener的size不为空，导致执行了runBlockingCatching带来异常，就说明代码有生命周期问题
   */
  fun emitChangeSync() = if (signal.size > 0) {
//    val ctx = if (context.isActive) {
//      context
//    } else {
//      debugger()
//      ioAsyncExceptionHandler
//    }
    runBlockingCatching(context) {
      emitChange()
    }.getOrThrow()
  } else {
  }

  suspend fun emitChange() {
    signal.emit(self)
  }
}