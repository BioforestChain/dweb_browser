package org.dweb_browser.helper

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

public val commonAsyncExceptionHandler: CoroutineExceptionHandler =
  CoroutineExceptionHandler { ctx, e ->
    printError(ctx.toString(), e.message, e)
    debugger(ctx, e)
  }
public val globalEmptyScope: CoroutineScope =
  CoroutineScope(EmptyCoroutineContext + commonAsyncExceptionHandler)

public val defaultAsyncExceptionHandler: CoroutineContext =
  Dispatchers.Default + commonAsyncExceptionHandler
public val globalDefaultScope: CoroutineScope = CoroutineScope(defaultAsyncExceptionHandler)

public val defaultUnconfinedExceptionHandler: CoroutineContext =
  Dispatchers.Unconfined + commonAsyncExceptionHandler
public val globalUnconfinedScope: CoroutineScope = CoroutineScope(defaultUnconfinedExceptionHandler)

//val ioAsyncExceptionHandler = Dispatchers.IO + commonAsyncExceptionHandler
public val mainAsyncExceptionHandler: CoroutineContext =
  SupervisorJob() + Dispatchers.Main + commonAsyncExceptionHandler
public val globalMainScope: CoroutineScope = CoroutineScope(mainAsyncExceptionHandler)

public expect val ioAsyncExceptionHandler: CoroutineContext
private var gioScope: CoroutineScope? = null
public val globalIoScope: CoroutineScope
  get() = gioScope ?: CoroutineScope(ioAsyncExceptionHandler).also { gioScope = it }

public expect suspend inline fun <T> withMainContext(crossinline block: suspend () -> T): T
public suspend inline fun <T> withMainContextCommon(crossinline block: suspend () -> T): T {
  return withContext(mainAsyncExceptionHandler) { block() }
}

public suspend inline fun <T> withIoContext(crossinline block: suspend () -> T): T {
  return withContext(ioAsyncExceptionHandler) { block() }
}

public suspend inline fun <T> withScope(
  scope: CoroutineScope,
  noinline block: suspend CoroutineScope.() -> T,
): T = withContext(scope.coroutineContext, block)

public inline fun CoroutineScope.launchWithMain(
  crossinline block: suspend () -> Unit,
): Job = launch { withMainContext(block) }