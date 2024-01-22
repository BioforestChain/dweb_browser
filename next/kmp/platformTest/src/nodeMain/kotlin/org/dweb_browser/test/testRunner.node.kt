package org.dweb_browser.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(DelicateCoroutinesApi::class)
actual fun runCommonTest(
  context: CoroutineContext?,
  block: suspend CoroutineScope.() -> Unit
) {
  GlobalScope.promise(context ?: EmptyCoroutineContext, block = block)
}