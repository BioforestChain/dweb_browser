package org.dweb_browser.helper

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun now() = datetimeNow().formatTimestampByMilliseconds()
  .padEndAndSub(23) // kmp中LocalDateTime跟android不一样 // LocalDateTime.toString().padEndAndSub(23)

fun printError(tag: String, msg: Any?, err: Any? = null) {
  when (err) {
    null -> println("${tag.padEnd(60, ' ')} $msg")
    is Throwable -> {
      eprintln("${tag.padEnd(60, ' ')} $msg")
      err.printStackTrace()
    }

    else -> eprintln("${tag.padEnd(60, ' ')} $msg $err")
  }
}

fun debugger(@Suppress("UNUSED_PARAMETER") vararg params: Any?) {
  println("DEBUGGER 请打断点")
}

expect fun eprintln(message: String)

fun WARNING(message: String) = eprintln("WARNING: $message")

val commonAsyncExceptionHandler = CoroutineExceptionHandler { ctx, e ->
  printError(ctx.toString(), e.message, e)
  debugger(ctx, e)
}
val defaultAsyncExceptionHandler = Default + commonAsyncExceptionHandler

//val ioAsyncExceptionHandler = Dispatchers.IO + commonAsyncExceptionHandler
val mainAsyncExceptionHandler = SupervisorJob() + Main + commonAsyncExceptionHandler
expect val ioAsyncExceptionHandler: CoroutineContext

val emptyScope = CoroutineScope(EmptyCoroutineContext + commonAsyncExceptionHandler)

expect suspend inline fun <T> withMainContext(crossinline block: suspend () -> T): T


suspend inline fun <T> withMainContextCommon(crossinline block: suspend () -> T): T {
  return if (Main.isDispatchNeeded(EmptyCoroutineContext)) {
    withContext(mainAsyncExceptionHandler) { block() }
  } else {
    block()
  }
}

suspend fun <T> withScope(
  scope: CoroutineScope,
  block: suspend CoroutineScope.() -> T,
) = withContext(scope.coroutineContext, block)
//  scope.async(block=block).await()

inline fun CoroutineScope.launchWithMain(
  crossinline block: suspend () -> Unit,
) = launch { withMainContext(block) }


private val times = mutableMapOf<String, String>()
fun timeStart(label: String) {
  times[label] = LocalDateTime.toString()
}

fun timeEnd(label: String) {
  times.remove(label)?.also { startTime ->
    val endTime = LocalDateTime.toString()
    val betweenTime =
      LocalDateTime.parse(endTime).nanosecond - LocalDateTime.parse(startTime).nanosecond
    printDebug(
      "TIME-DURATION", label, "${betweenTime / 1000000.0}ms"
    )
  }
}

/**
 * 可用值：
 *
 * "TIME-DURATION"
 *
 * "fetch"
 * "stream"
 * "native-ipc"
 * "stream-ipc"
 * "jmm"
 * "boot"
 * "http"
 * "mwebview"
 * "dwebview"
 * "js-process"
 * "message-port-ipc"
 * "ipc-body"
 * "nativeui"
 * "fetch-file"
 *
 */
private val debugTagsRegex = mutableSetOf<Regex>()
private val debugTags = mutableSetOf<String>()

fun addDebugTags(tags: Iterable<String>) {
  for (tag in tags) {
    if (tag.isEmpty()) {
      continue
    }
    if (tag.startsWith('/') && tag.endsWith('/')) {
      debugTagsRegex.add(Regex(tag.slice(1..<tag.length - 1)))
    } else {
      debugTags.add(tag.trim())
    }
  }
}

fun isScopeEnableDebug(scope: String) =
  debugTags.contains(scope) || debugTagsRegex.firstOrNull { regex -> regex.matches(scope) } != null

fun printDebug(scope: String, tag: String, message: Any?, error: Any? = null) {
  if (error == null && !isScopeEnableDebug(scope)) {
    return
  }
  var err = error;
  val msg = when (message) {
    is Lazy<*> -> {
      try {
        message.value
      } catch (e: Throwable) {
        err = e
      }
    }

    else -> message
  }
  printError(
    "${now()} | ${
      when (scope.length) {
        in 0..16 -> scope.padEndAndSub(16)
        in 16..32 -> scope.padEndAndSub(32)
        in 32..48 -> scope.padEndAndSub(48)
        else -> scope.padEndAndSub(60)
      }
    } | ${tag.padEndAndSub(22)} |", msg, err
  )
}

fun String.padEndAndSub(length: Int): String {
  return this.padEnd(length, ' ').substring(0, length)
}

class Debugger(val scope: String) {
  operator fun invoke(tag: String, msg: Any? = "", err: Any? = null) {
    printDebug(scope, tag, msg, err)
  }

  inline operator fun invoke(tag: String, err: Throwable, msgGetter: () -> Any?) {
    if (isScopeEnableDebug(scope)) {
      val msg = try {
        msgGetter()
      } catch (e: Throwable) {
        e
      }
      printDebug(scope, tag, msg, err)
    }
  }

  inline operator fun invoke(tag: String, msgGetter: () -> Any?) {
    if (isScopeEnableDebug(scope)) {
      var err: Throwable? = null
      val msg = try {
        msgGetter()
      } catch (e: Throwable) {
        err = e
        null
      }
      printDebug(scope, tag, msg, err)
    }
  }

  fun forceEnable(enable: Boolean = true) {
    if (enable) {
      debugTags.add(scope)
    } else {
      debugTags.remove(scope)
    }
  }

  val isEnable
    get() = debugTags.contains(scope) || debugTagsRegex.firstOrNull { regex ->
      regex.matches(
        scope
      )
    } != null
}

val debugTest = Debugger("test")
val debugTimeout = Debugger("timeout")
suspend inline fun <R> traceTimeout(
  ms: Long,
  crossinline log: () -> Any?,
  crossinline block: suspend () -> R,
) = if (debugTimeout.isEnable) {
  coroutineScope {
    val timeoutJob = launch { delay(ms);debugTimeout("traceTimeout", msgGetter = log) }
    try {
      block()
    } finally {
      timeoutJob.cancel()
    }
  }
} else {
  block()
}