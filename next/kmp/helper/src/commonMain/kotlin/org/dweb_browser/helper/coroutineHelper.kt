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
val globalEmptyScope = CoroutineScope(EmptyCoroutineContext + commonAsyncExceptionHandler)

val defaultAsyncExceptionHandler = Dispatchers.Default + commonAsyncExceptionHandler
val globalDefaultScope = CoroutineScope(defaultAsyncExceptionHandler)

val defaultUnconfinedExceptionHandler = Dispatchers.Unconfined + commonAsyncExceptionHandler
val globalUnconfinedScope = CoroutineScope(defaultUnconfinedExceptionHandler)

//val ioAsyncExceptionHandler = Dispatchers.IO + commonAsyncExceptionHandler
val mainAsyncExceptionHandler = SupervisorJob() + Dispatchers.Main + commonAsyncExceptionHandler
val globalMainScope = CoroutineScope(mainAsyncExceptionHandler)

expect val ioAsyncExceptionHandler: CoroutineContext
private var _ioScope: CoroutineScope? = null
val globalIoScope get() = _ioScope ?: CoroutineScope(ioAsyncExceptionHandler).also { _ioScope = it }
expect suspend inline fun <T> withMainContext(crossinline block: suspend () -> T): T
suspend inline fun <T> withMainContextCommon(crossinline block: suspend () -> T): T {
  return if (Dispatchers.Main.isDispatchNeeded(EmptyCoroutineContext)) {
    withContext(mainAsyncExceptionHandler) { block() }
  } else {
    block()
  }
}

suspend inline fun <T> withIoContext(crossinline block: suspend () -> T): T {
  return withContext(ioAsyncExceptionHandler) { block() }
}

suspend inline fun <T> withScope(
  scope: CoroutineScope,
  noinline block: suspend CoroutineScope.() -> T,
) = withContext(scope.coroutineContext, block)

inline fun CoroutineScope.launchWithMain(
  crossinline block: suspend () -> Unit,
) = launch { withMainContext(block) }