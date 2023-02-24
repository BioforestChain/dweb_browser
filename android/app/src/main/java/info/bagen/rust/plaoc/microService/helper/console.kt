package info.bagen.rust.plaoc.microService.helper

import java.time.LocalDateTime

inline fun now() = LocalDateTime.now().toString().padEnd(26, '0').slice(0..25)

inline fun printerrln(log: String) = System.err.println(log)
inline fun printerrln(tag: String, msg: Any?, err: Throwable? = null) {
    printerrln("$tag\t $msg")
    err?.printStackTrace()
}

/**
 * 可用值：
 * "fetch", "stream", "native-ipc"
 */
val debugTags by lazy {
    (System.getProperty("dweb-debug") ?: "").let {
        it.split(" ")
            .filter { s -> s.isNotEmpty() }
            .toSet()
    }
//    setOf<String>()
}

inline fun printdebugln(scope: String, tag: String, msg: Any?, err: Throwable? = null) {
    if (!debugTags.contains(scope)) {
        return
    }
    printerrln("${now()}\t│ $scope\t│ $tag", msg, err)
}