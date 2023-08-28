package info.bagen.dwebbrowser

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

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


  @Test
  fun testMultiAwait() = runBlocking {
    val po = PromiseOut<Unit>()
    val startTime = System.currentTimeMillis()

    launch {
      delay(1000)
      po.resolve(Unit)
    }
    launch {
      delay(1000)
      po.resolve(Unit)
    }
    launch {
      println("start wait 1")
      po.waitPromise()
      println("resolved 1")
    }

    launch {
      println("start wait 2")
      po.waitPromise()
      println("resolved 2")
    }

    println("start wait 3")
    po.waitPromise()
    println("resolved 3")

    assertEquals(System.currentTimeMillis() - startTime >= 1000L, true)
  }

  @Test
  fun bench() = runBlocking {
    println("start")

    val TIMES = 10000

    val result1 = AtomicInteger(0)
    val result2 = AtomicInteger(0)
    val ioAsyncScope = MainScope() + ioAsyncExceptionHandler
    for (i in 1..TIMES) {
      val po = PromiseOut<Unit>()
      ioAsyncScope.launch {
        delay(100)
        result1.addAndGet(1)
        po.resolve(Unit)
      }
      ioAsyncScope.launch {
        po.waitPromise()
        result2.addAndGet(1)
      }
    }



    while (result2.get() < TIMES) {
      delay(200)
      println("times result1:${result1.get()} result2:${result2.get()}")

    }
    assertEquals(result1.get(), result2.get())
  }
}