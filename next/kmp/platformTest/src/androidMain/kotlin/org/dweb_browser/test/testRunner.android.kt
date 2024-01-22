package org.dweb_browser.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

actual fun runCommonTest(
  context: CoroutineContext?,
  block: suspend CoroutineScope.() -> Unit
) {
  runBlocking(context ?: EmptyCoroutineContext, block)
}