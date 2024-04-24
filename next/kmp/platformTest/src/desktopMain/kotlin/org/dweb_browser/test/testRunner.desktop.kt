package org.dweb_browser.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.debug.DebugProbes
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
actual fun runCommonTest(
  context: CoroutineContext?, timeout: Duration?, block: suspend CoroutineScope.() -> Unit,
) {
  DebugProbes.install()
  defaultRunCommonTest(context, timeout, block)
}

@OptIn(ExperimentalCoroutinesApi::class)
actual fun dumpCoroutines() {
  DebugProbes.dumpCoroutines()
}