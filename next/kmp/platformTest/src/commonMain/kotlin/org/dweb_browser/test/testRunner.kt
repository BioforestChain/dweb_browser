package org.dweb_browser.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

/**
 * 执行异步测试，默认不会使用虚拟时间，详见 https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/
 */
expect fun runCommonTest(
  context: CoroutineContext? = null,
  timeout: Duration? = null,
  block: suspend CoroutineScope.() -> Unit,
): TestResult

expect fun dumpCoroutines()

internal fun defaultRunCommonTest(
  context: CoroutineContext?, timeout: Duration?, block: suspend CoroutineScope.() -> Unit,
) = (context ?: EmptyCoroutineContext).let { ctx ->
  when (timeout) {
    null -> runTest(context = ctx) {
      /// 避免虚拟时间
      withContext(Dispatchers.Default) { block() }
    }

    else -> runTest(context = ctx, timeout = timeout) {
      /// 避免虚拟时间
      withContext(Dispatchers.Default) { block() }
    }
  }
}


fun runCommonTest(
  times: Int,
  context: CoroutineContext? = null,
  timeout: Duration? = null,
  block: suspend CoroutineScope.(Int) -> Unit,
) {
  for (i in 1..times) {
    runCommonTest(context, timeout) { block(i) }
  }
}