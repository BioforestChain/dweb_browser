package org.dweb_browser.test

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

actual fun runCommonTest(
  context: CoroutineContext?, timeout: Duration?, block: suspend CoroutineScope.() -> Unit,
) = defaultRunCommonTest(context, timeout, block)

actual fun dumpCoroutines() {
}