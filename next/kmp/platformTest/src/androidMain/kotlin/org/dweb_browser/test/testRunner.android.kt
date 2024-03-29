package org.dweb_browser.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

actual fun runCommonTest(
  context: CoroutineContext?,
  timeout: Duration?,
  block: suspend CoroutineScope.() -> Unit,
) = defaultRunCommonTest(context, timeout, block)