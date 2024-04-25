package org.dweb_browser.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

actual fun runCommonTest(
  context: CoroutineContext?,
  timeout: Duration?,
  block: suspend CoroutineScope.() -> Unit,
) = run {
  val appContext = ApplicationProvider.getApplicationContext<Context>()
  defaultRunCommonTest(context, timeout, block)
}

actual fun dumpCoroutines() {
}