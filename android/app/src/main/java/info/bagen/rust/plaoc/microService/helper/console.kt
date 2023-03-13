package info.bagen.rust.plaoc.microService.helper

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope.coroutineContext
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.LocalDateTime
import kotlin.coroutines.CoroutineContext

inline fun now() = LocalDateTime.now().toString().padEnd(26, '0').slice(0..25)

inline fun printerrln(log: String) = System.err.println(log)
inline fun printerrln(tag: String, msg: Any?, err: Throwable? = null) {
    printerrln("$tag\t $msg")
    err?.printStackTrace()
}

inline fun debugger(vararg params: Any?) {
    for (p in params) {

    }
    println("DEBUGGER")
}

val commonAsyncExceptionHandler = CoroutineExceptionHandler { ctx, e ->
    printerrln(ctx.toString(), e.message, e)
    debugger(ctx, e)
}
val ioAsyncExceptionHandler = Dispatchers.IO + commonAsyncExceptionHandler
val mainAsyncExceptionHandler = Dispatchers.Main + commonAsyncExceptionHandler
fun <T> runBlockingCatching(
    context: CoroutineContext, block: suspend CoroutineScope.() -> T
) = kotlin.runCatching {
    runBlocking(context, block)
}.onFailure {
    commonAsyncExceptionHandler.handleException(context, it)
}

fun <T> runBlockingCatching(
    block: suspend CoroutineScope.() -> T
) = kotlin.runCatching {
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
 * "systemui"
 *
 */
val debugTags by lazy {
    (System.getProperty("dweb-debug") ?: "").let {
        it.split(" ").filter { s -> s.isNotEmpty() }.toMutableSet()
    }
//    setOf<String>()
}

inline fun printdebugln(scope: String, tag: String, msg: Any?, err: Throwable? = null) {
    if (!debugTags.contains(scope)) {
        return
    }
    printerrln("${now()}\t│ $scope\t│ $tag", msg, err)
}