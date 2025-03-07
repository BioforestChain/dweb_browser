package org.dweb_browser.test

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

//@RunWith(AndroidJUnit4::class)
actual fun runCommonTest(
  context: CoroutineContext?,
  timeout: Duration?,
  block: suspend CoroutineScope.() -> Unit,
) = run {
//  val appContext = ApplicationProvider.getApplicationContext<Context>()
  defaultRunCommonTest(context, timeout, block)
}

actual fun dumpCoroutines() {
}