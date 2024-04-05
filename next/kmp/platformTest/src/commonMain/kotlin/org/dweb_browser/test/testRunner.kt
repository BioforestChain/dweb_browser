package org.dweb_browser.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestResult
import kotlin.coroutines.CoroutineContext

/**
 * 执行异步测试，默认不会使用虚拟时间，详见 https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/
 */
expect fun runCommonTest(
  context: CoroutineContext? = null,
  block: suspend CoroutineScope.() -> Unit
): TestResult

fun runCommonTest(
  times: Int,
  context: CoroutineContext? = null,
  block: suspend CoroutineScope.(Int) -> Unit
) {
  for (i in 1..times) {
    runCommonTest(context) { block(i) }
  }
}