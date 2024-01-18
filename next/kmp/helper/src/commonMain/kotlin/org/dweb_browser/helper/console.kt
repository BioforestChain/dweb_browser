package org.dweb_browser.helper

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

fun now() = datetimeNow().formatTimestampByMilliseconds()
  .padEndAndSub(23) // kmp中LocalDateTime跟android不一样 // LocalDateTime.toString().padEndAndSub(23)

fun printError(tag: String, msg: Any?, err: Throwable? = null) {
  println("${tag.padEnd(60, ' ')} $msg")
  err?.printStackTrace()
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

@OptIn(ExperimentalStdlibApi::class)
suspend inline fun <T> withMainContext(crossinline block: suspend () -> T): T {
  return if (coroutineContext[CoroutineDispatcher.Key] == Main) {
    block()
  } else {
    withContext(mainAsyncExceptionHandler) { block() }
  }
}

inline fun CoroutineScope.launchWithMain(
  crossinline block: suspend () -> Unit
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

class addDebugTags {
  suspend fun invoke(tags: Iterable<String>) {
    for (tag in tags) {
      if (tag.isEmpty()) {
        continue
      }
      if (tag.startsWith('/') && tag.endsWith('/')) {
        println("DEBUG!! ${tag.slice(1 until tag.length - 2)}")
        debugTagsRegex.add(Regex(tag.slice(1 until tag.length - 2)))
      } else {
        debugTags.add(tag.trim())
      }
    }
  }

  companion object {
    init {
      // TODO: System.getProperty 未处理
//      addDebugTags((System.getProperty("dweb-debug") ?: "").split(" "))
    }
  }
}

fun addDebugTags(tags: Iterable<String>) {
  for (tag in tags) {
    if (tag.isEmpty()) {
      continue
    }
    if (tag.startsWith('/') && tag.endsWith('/')) {
      debugTagsRegex.add(Regex(tag.slice(1..tag.length - 2)))
    } else {
      debugTags.add(tag.trim())
    }
  }
}

fun isScopeEnableDebug(scope: String) =
  debugTags.contains(scope) || debugTagsRegex.firstOrNull { regex -> regex.matches(scope) } != null

fun printDebug(scope: String, tag: String, message: Any?, error: Throwable? = null) {
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
  printError("${now()} | ${scope.padEndAndSub(16)} | ${tag.padEndAndSub(22)} |", msg, err)
}

fun String.padEndAndSub(length: Int): String {
  return this.padEnd(length, ' ').substring(0, length)
}

class Debugger(val scope: String) {
  operator fun invoke(tag: String, msg: Any? = "", err: Throwable? = null) {
    printDebug(scope, tag, msg, err)
  }

  inline operator fun invoke(tag: String, err: Throwable?, msgGetter: () -> Any?) {
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
      var error: Throwable? = null
      val msg = try {
        msgGetter()
      } catch (e: Throwable) {
        error = e
        null
      }
      printDebug(scope, tag, msg, error)
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
