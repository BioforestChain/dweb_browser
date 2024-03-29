package org.dweb_browser.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlinx.coroutines.test.TestResult
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(DelicateCoroutinesApi::class)
actual fun runCommonTest(
  context: CoroutineContext?,
  timeOut: Duration?,
  block: suspend CoroutineScope.() -> Unit
): TestResult {
//  TestScope(context ?: EmptyCoroutineContext)
//    //
//    .runTest(testBody = block)
//  runTest(context ?: EmptyCoroutineContext, testBody = block)
  return GlobalScope.promise(context ?: EmptyCoroutineContext, block = block)
}