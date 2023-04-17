package info.bagen.rust.plaoc.learn

import info.bagen.rust.plaoc.AsyncBase
import info.bagen.dwebbrowser.microService.helper.now
import info.bagen.dwebbrowser.microService.helper.toByteArray
import info.bagen.dwebbrowser.microService.helper.toInt
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SynchronizedTest : AsyncBase() {

    @Test
    fun test() = runBlocking {

        suspend fun foo(v: Int) {
            val ba = v.toByteArray()
            val v2 = ba.toInt()
            println("${now()} ${ba.joinToString { it.toUByte().toString() }}/$v2")
            assertEquals(v, v2)
        }

        for (x in 1..100) {
            deferredList += async {
                for (y in 1..100) {
                    foo(x * y)
                }
            }
        }
        prepareReady.complete(Unit)

        awaitAll()
    }
}