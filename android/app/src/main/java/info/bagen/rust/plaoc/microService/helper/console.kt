package info.bagen.rust.plaoc.microService.helper

inline fun printerrln(log: String) = System.err.println(log)
inline fun printerrln(tag: String, msg: Any, err: Throwable? = null) {
    printerrln("$tag\t $msg")
    err?.printStackTrace()
}

/**
 * 可用值：
 * "fetch", "stream", "native-ipc"
 */
val debugTags by lazy {
    (System.getProperty("dweb-debug") ?: "").let { it.split(" ").filter { it.length > 0 }.toSet() }
//    setOf<String>()
}

inline fun printdebugln(scope: String, tag: String, msg: Any, err: Throwable? = null) {
    if (!debugTags.contains(scope)) {
        return
    }
    printerrln("$scope:$tag", msg, err)
}