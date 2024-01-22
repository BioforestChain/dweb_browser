package org.dweb_browser.test

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

expect fun runCommonTest(
  context: CoroutineContext? = null,
  block: suspend CoroutineScope.() -> Unit
): Unit
