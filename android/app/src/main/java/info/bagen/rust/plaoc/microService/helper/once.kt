package info.bagen.rust.plaoc.microService.helper

//inline fun <R> once(noinline runnable:  () -> R):  () -> R{
//    var runned = false
//    var result: Any? = null
//    return {
//        if (runned === false) {
//            runned = true
//            result = runnable()
//        }
//        result as R
//    }
//}

@Synchronized
inline fun <R> suspendOnce(noinline runnable: suspend () -> R): suspend () -> R {
    var runned = false
    var result: Any? = null
    return {
        if (runned === false) {
            runned = true
            result = runnable()
        }
        result as R
    }
}