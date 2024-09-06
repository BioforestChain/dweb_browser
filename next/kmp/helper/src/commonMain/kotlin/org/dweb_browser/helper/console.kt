package org.dweb_browser.helper

public fun now(): String = datetimeNow().formatTimestampByMilliseconds()
  .padEndAndSub(23) // kmp中LocalDateTime跟android不一样 // LocalDateTime.toString().padEndAndSub(23)

public fun printError(tag: String, msg: Any?, err: Any? = null) {
  when (err) {
    null -> println("${tag.padEnd(60, ' ')} $msg")
    is Throwable -> {
      eprintln("${tag.padEnd(60, ' ')} $msg")
      err.printStackTrace()
    }

    else -> eprintln("${tag.padEnd(60, ' ')} $msg $err")
  }
}

public fun debugger(@Suppress("UNUSED_PARAMETER") vararg params: Any?) {
  println("DEBUGGER 请打断点")
}

public expect fun eprintln(message: String)

public fun WARNING(message: Any?) {
  val msg = if (message is Throwable) {
    message.stackTraceToString()
  } else message.toString()
  val datetime = now()
  eprintln(msg.split("\n").joinToString("\n") { if (it.isEmpty()) it else "$datetime | ⚠️ | $it" })
}


public fun printDebug(
  scope: String,
  tag: String,
  message: Any?,
  error: Any? = null,
  symbol: String = "|",
) {
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
    "${now()} $symbol ${scope.tabStringify()} $symbol ${tag.tabStringify()} $symbol", msg, err
  )
}

public fun String.padEndAndSub(length: Int): String {
  return this.padEnd(length, ' ').substring(0, length)
}

private fun String.tabStringify() = when (length) {
  in 0..8 -> padEndAndSub(8)
  in 8..16 -> padEndAndSub(16)
  in 16..24 -> padEndAndSub(24)
  in 16..32 -> padEndAndSub(32)
  in 32..40 -> padEndAndSub(40)
  in 40..48 -> padEndAndSub(48)
  else -> padEndAndSub(56)
}

public val debugTest: Debugger = Debugger("test")