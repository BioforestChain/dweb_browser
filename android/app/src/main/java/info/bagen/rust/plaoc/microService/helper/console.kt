package info.bagen.rust.plaoc.microService.helper

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope.coroutineContext
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.LocalDateTime
import kotlin.coroutines.CoroutineContext

fun now() = LocalDateTime.now().toString()//.padEnd(23, '0')

fun printerrln(tag: String, msg: Any?, err: Throwable? = null) {
    System.err.println("${tag.padEnd(60, ' ')} $msg")
    err?.printStackTrace()
}

/*fun debugger(vararg params: Any?) {
    println("DEBUGGER")
}*/

val commonAsyncExceptionHandler = CoroutineExceptionHandler { ctx, e ->
    printerrln(ctx.toString(), e.message, e)
    //debugger(ctx, e)
}
val ioAsyncExceptionHandler = Dispatchers.IO + commonAsyncExceptionHandler
val mainAsyncExceptionHandler = Dispatchers.Main + commonAsyncExceptionHandler
fun <T> runBlockingCatching(
    context: CoroutineContext, block: suspend CoroutineScope.() -> T
) = runCatching {
    runBlocking(context, block)
}.onFailure {
    commonAsyncExceptionHandler.handleException(context, it)
}

fun <T> runBlockingCatching(
    block: suspend CoroutineScope.() -> T
) = runCatching {
    runBlocking { block() }
}.onFailure {
    commonAsyncExceptionHandler.handleException(coroutineContext, it)
}

private val times = mutableMapOf<String, LocalDateTime>()
fun timeStart(label: String) {
    times[label] = LocalDateTime.now()
}

fun timeEnd(label: String) {
    times.remove(label)?.also { startTime ->
        val endTime = LocalDateTime.now()
        printdebugln(
            "TIME-DURATION",
            label,
            "${Duration.between(startTime, endTime).toNanos() / 1000000.0}ms"
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
val debugTags by lazy {
    (System.getProperty("dweb-debug") ?: "").let {
        it.split(" ").filter { s -> s.isNotEmpty() }.toMutableSet()
    }
//    setOf<String>()
}

fun printdebugln(scope: String, tag: String, msg: Any?, err: Throwable? = null) {
    if (!debugTags.contains(scope)) {
        return
    }
    printerrln("${now()} │ ${scope.padEnd(8, ' ')} │ $tag", msg, err)
}