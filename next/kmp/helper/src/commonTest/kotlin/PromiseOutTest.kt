package info.bagen.dwebbrowser

import org.dweb_browser.helper.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals

class PromiseOutTest {
  @Test
  fun testResolve() = runBlocking {
    val po = PromiseOut<Unit>()

    val startTime = Clock.System.now().toEpochMilliseconds()

    launch {
      delay(1000)
      po.resolve(Unit)
    }

    println("start wait")
    po.waitPromise()
    println("resolved")

    assertEquals(Clock.System.now().toEpochMilliseconds() - startTime >= 1000L, true)
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
    val startTime = Clock.System.now().toEpochMilliseconds()

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

    assertEquals(Clock.System.now().toEpochMilliseconds() - startTime >= 1000L, true)
  }

  @Test
  fun bench() = runBlocking {
    println("start")

    val TIMES = 10000;


    val result1 = AtomicInteger(0)
    val result2 = AtomicInteger(0)
    for (i in 1..TIMES) {
      val po = PromiseOut<Unit>()
      GlobalScope.launch {
        delay(100)
        result1.addAndGet(1)
        po.resolve(Unit)
      }
      GlobalScope.launch {
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