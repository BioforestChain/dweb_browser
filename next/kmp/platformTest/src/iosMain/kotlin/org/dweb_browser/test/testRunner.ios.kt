package org.dweb_browser.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

actual fun runCommonTest(
  context: CoroutineContext?, block: suspend CoroutineScope.() -> Unit
) = runTest(context = context ?: EmptyCoroutineContext) {
  /// 避免虚拟时间
  withContext(Dispatchers.Default) { block() }
}