import ReadableStream
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * You can edit, run, and share this code.
 * play.kotlinlang.org
 */
fun main() {
    var b: Byte = 0
    val stream = ReadableStream(onPull = { (_, controller) ->
        Thread.sleep(500)
        controller.enqueue(byteArrayOf(b++))
        if (b > 10) {
            controller.close()
        }
    });


    GlobalScope.launch {
        println("reading")
        while (stream.available() > 0) {
            println("read ${stream.read()}")
        }
    }
    runBlocking {
        stream.afterClosed()
    }
    println("DONE")
}
