package org.dweb_browser.helper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

/**
 * 策略：
 * 1. *默认* 放弃排队
 * 2. 与之前排队的合并成一个
 * 3. 不合并，排队执行
 */
class Queue<R>(
  private val strategy: StrategyContext<R>.() -> Deferred<R>,
  private val block: suspend () -> R,
) {
  class StrategyContext<R>(
    override val coroutineContext: CoroutineContext,
    val enqueue: () -> Deferred<R>,
    val current: Deferred<R>,
    val pending: List<Deferred<R>>,
  ) : CoroutineScope

  private val _all = mutableListOf<Deferred<R>>()
  private val mutex = Mutex()
  private fun CoroutineScope.create() =
    when {
      // Busy
      _all.isNotEmpty() -> StrategyContext(
        coroutineContext,
        {
          async {
            runCatching { _all.lastOrNull()?.await() }.getOrNull()
            block()
          }
        },
        _all.first(),
        _all.slice(1 until _all.size),
      ).strategy()

      else -> async { block() }
    }

  suspend operator fun invoke(): R {
    return mutex.withLock {
      coroutineScope {
        create().also { result ->
          if (!_all.contains(result)) {
            _all.add(result)
            result.invokeOnCompletion {
              _all.remove(result)
            }
          }
        }
      }
    }.await()
  }
  companion object {
    /**
     * 放弃排队
     */
    fun <R> drop(block: suspend () -> R) = Queue({ current }, block)

    /**
     * 与之前排队的合并成一个
     */
    fun <R> merge(block: suspend () -> R) = Queue({
      when (val last = pending.firstOrNull()) {
        null -> enqueue()
        else -> last
      }
    }, block)

    /**
     * 不合并，排队执行
     */
    fun <R> enqueue(block: suspend () -> R) = Queue({
      enqueue()
    }, block)
  }
}