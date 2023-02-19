import info.bagen.rust.plaoc.microService.ipc.ReadableStream
import info.bagen.rust.plaoc.microService.ipc.streamAsRawData
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.stream.Stream

/**
 * You can edit, run, and share this code.
 * play.kotlinlang.org
 */
fun main() {
    var b: Byte = 0
    val stream = ReadableStream(onPull = { controller ->
        Thread.sleep(500)
        controller.enqueue(byteArrayOf(b++))
        if (b > 10) {
            controller.close()
        }
    });


    GlobalScope.launch {
        println("reading")
        while (stream.available()>0) {
            println("read ${stream.read()}")
        }
    }
    runBlocking {
        stream.closed()
    }
    println("DONE")
}
