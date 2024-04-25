package org.dweb_browser.helper

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

val commonAsyncExceptionHandler = CoroutineExceptionHandler { ctx, e ->
  printError(ctx.toString(), e.message, e)
  debugger(ctx, e)
}
val defaultAsyncExceptionHandler = Dispatchers.Default + commonAsyncExceptionHandler

//val ioAsyncExceptionHandler = Dispatchers.IO + commonAsyncExceptionHandler
val mainAsyncExceptionHandler = SupervisorJob() + Dispatchers.Main + commonAsyncExceptionHandler
expect val ioAsyncExceptionHandler: CoroutineContext
val emptyScope = CoroutineScope(EmptyCoroutineContext + commonAsyncExceptionHandler)
expect suspend inline fun <T> withMainContext(crossinline block: suspend () -> T): T
suspend inline fun <T> withMainContextCommon(crossinline block: suspend () -> T): T {
  return if (Dispatchers.Main.isDispatchNeeded(EmptyCoroutineContext)) {
    withContext(mainAsyncExceptionHandler) { block() }
  } else {
    block()
  }
}

suspend fun <T> withScope(
  scope: CoroutineScope,
  block: suspend CoroutineScope.() -> T,
) = withContext(scope.coroutineContext, block)

inline fun CoroutineScope.launchWithMain(
  crossinline block: suspend () -> Unit,
) = launch { withMainContext(block) }