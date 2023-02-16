package info.bagen.rust.plaoc.microService.ipc.helper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.junit.Test
import java.io.PipedInputStream
import java.io.PipedOutputStream

class ReadableStream {
    private val writer = PipedOutputStream()
    private val reader = PipedInputStream(writer)
    private val isNext = Mutex()

    init {
        runBlocking {
            isNext.lock()
        }
    }


    /** read()*/
    suspend fun onMessage(): Pair<ByteArray, Boolean> {
        isNext.unlock()
        val byte = reader.readBytes()
        isNext.lock()
        return Pair(byte, false)
    }

    suspend fun postMessage(data: ByteArray) {
        if (isNext.isLocked) {
            isNext.lock()
        }
        return withContext(Dispatchers.IO) {
            writer.write(data)
        }
    }


    fun close() {
        writer.close()
        reader.close()
    }

}

@Test
suspend fun testReadableStream() {
    val stream = ReadableStream()
    stream.postMessage(ByteArray(100))
    val (bytes,next) =  stream.onMessage()
   while (next) {
       println("testReadableStream ==> $bytes")
   }
}

fun main() {
    runBlocking {
        testReadableStream()
    }
}
