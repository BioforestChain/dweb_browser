package info.bagen.kmmsharedmodule.helper

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope.coroutineContext
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds


//inline fun now() = LocalDateTime.now().toString().padEnd(26, '0').slice(0..25)
inline fun now() = Clock.System.now()
    .toLocalDateTime(timeZone = TimeZone.currentSystemDefault()).toString()
    .padEnd(26, '0').slice(0..25)
inline fun localDateTimeNow() = Clock.System.now().toLocalDateTime(timeZone = TimeZone.currentSystemDefault())

inline fun printerrln(log: String) = System.err.println(log)
inline fun printerrln(tag: String, msg: Any?, err: Throwable? = null) {
    printerrln("$tag\t $msg")
    err?.printStackTrace()
}

inline fun debugger(vararg params: Any) {
    for (p in params) {

    }
    println("DEBUGGER")
}

val commonAsyncExceptionHandler = CoroutineExceptionHandler { ctx, e ->
    printerrln(ctx.toString(), e.message, e)
    debugger(ctx, e)
}

val ioAsyncExceptionHandler = Dispatchers.IO + commonAsyncExceptionHandler
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
//    times[label] = LocalDateTime.now()
    times[label] = localDateTimeNow()
}

fun timeEnd(label: String) {
    times.remove(label)?.also { startTime ->
//        val endTime = LocalDateTime.now()
        val endTime = localDateTimeNow()

        val startDuration = Duration.parse(startTime.toString())
        val endDuration = Duration.parse(endTime.toString())


        printdebugln(
            "TIME-DURATION",
            label,
            "${endDuration.compareTo(startDuration).nanoseconds.inWholeNanoseconds.div(1000000.0)}ms"
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
 * "js-process"
 * "message-port-ipc"
 * "ipc-body"
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