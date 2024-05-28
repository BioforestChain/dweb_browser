package org.dweb_browser.helper

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime

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

fun WARNING(message: Any?) {
  val msg = if (message is Throwable) {
    message.stackTraceToString()
  } else message.toString()
  val datetime = now()
  eprintln(msg.split("\n").joinToString("\n") { if (it.isEmpty()) it else "$datetime | ⚠️ | $it" })
}

//  scope.async(block=block).await()


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

fun isScopeEnableVerbose(scope: String) = debugTags.contains(scope)
fun isScopeEnableTimeout(scope: String) = isScopeEnableDebug(scope)

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
  init {
    println("Debugger scope: $scope")
  }

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


  fun verbose(tag: String, msgGetter: () -> Any?) {
    if (isEnableVerbose) {
      invoke("VERBOSE $tag", msgGetter)
    }
  }

  fun verbose(tag: String, msg: Any? = "") {
    if (isEnableVerbose) {
      printDebug(scope, tag, msg)
    }
  }

  suspend inline fun <R> timeout(
    ms: Long,
    tag: String = "traceTimeout",
    crossinline log: () -> Any? = { "" },
    crossinline block: suspend () -> R,
  ) = if (isEnableTimeout) {
    val timeoutJob = CoroutineScope(currentCoroutineContext()).launch {
      delay(ms);
      printDebug(scope, tag, message = lazy { log() }, error = "⏲️ TIMEOUT!!")
    }
    try {
      block()
    } finally {
      timeoutJob.cancel()
    }
  } else {
    block()
  }

  inline fun timeout(
    scope: CoroutineScope,
    ms: Long,
    tag: String = "traceTimeout",
    crossinline log: () -> Any? = { "" },
  ) = CompletableDeferred<Unit>().let { deferred ->
    scope.launch(start = CoroutineStart.UNDISPATCHED) {
      timeout(ms, tag, log) {
        deferred.await()
      }
    }

    return@let {
      deferred.complete(Unit)
    }
  }


  val isEnableVerbose get() = isScopeEnableVerbose(scope)
  val isEnableTimeout get() = isScopeEnableTimeout(scope)

  val isEnable get() = isScopeEnableDebug(scope)
}

val debugTest = Debugger("test")