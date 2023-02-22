package info.bagen.rust.plaoc

import info.bagen.rust.plaoc.microService.helper.PromiseOut
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class PromiseOutTest {
    @Test
    fun testResolve() = runBlocking {
        val po = PromiseOut<Unit>()
        val startTime = System.currentTimeMillis()

        launch {
            delay(1000)
            po.resolve(Unit)
        }

        println("start wait")
        po.waitPromise()
        println("resolved")

        assertEquals(System.currentTimeMillis() - startTime >= 1000L, true)
    }

    @Test
    fun testReject() = runBlocking {
        val po = PromiseOut<Unit>()
        launch {
            delay(1000)
            po.reject(Exception("QAQ"))
        }

        println("start wait")
        try {
            po.waitPromise()
            throw Error("should no happened")
        } catch (e: Exception) {
            println("rejected")
            assertEquals(e.message, "QAQ")
        }
    }
}