package org.dweb_browser.test 

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

actual fun runCommonTest(
  context: CoroutineContext?,
  block: suspend CoroutineScope.() -> Unit
) = runTest(context = context ?: EmptyCoroutineContext, testBody = block)
