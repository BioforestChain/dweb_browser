package info.bagen.dwebbrowser.microService.helper

enum class ARG_COUNT {
    ONE
}


inline fun <A, R> suspendOnce(t: ARG_COUNT, noinline runnable: suspend (A) -> R): suspend (A) -> R {
    var runned = false
    var result: R? = null
    return {
        if (!runned) {
            runned = true
            result = runnable(it)
        }
        result as R
    }
}

inline fun <R> suspendOnce(noinline runnable: suspend () -> R): suspend () -> R {
    var runned = false
    var result: Any? = null
    return {
        if (!runned) {
            runned = true
            result = runnable()
        }
        result as R
    }
}