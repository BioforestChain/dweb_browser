package info.bagen.dwebbrowser

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.datetimeNow
import kotlin.test.Test
import kotlin.test.assertEquals

class PromiseOutTest {
  @Test
  fun testResolve() = runTest {
    val po = PromiseOut<Unit>()

    val startTime = datetimeNow()

    launch {
      delay(1000)
      po.resolve(Unit)
    }

    println("start wait")
    po.waitPromise()
    println("resolved")

    assertEquals(datetimeNow() - startTime >= 1000L, true)
  }

  @Test
  fun testReject() = runTest {
    val po = PromiseOut<Unit>()
    launch {
      delay(1000)
      po.reject(Exception("Reject"))
    }

    println("start wait")
    try {
      po.waitPromise()
      throw Error("should no happened")
    } catch (e: Exception) {
      println("rejected")
      assertEquals(e.message, "Reject")
    }
  }


  @Test
  fun testMultiAwait() = runTest {
    val po = PromiseOut<Unit>()
    val startTime = datetimeNow()

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

    assertEquals(datetimeNow() - startTime >= 1000L, true)
  }

  @Test
  @DelicateCoroutinesApi
  fun bench() = runTest {
    println("start")

    val TIMES = 10000;

    // 如果使用 atomic<Int>(0) 初始化，iOS测试会出现 result1 和 result2 最大值为128就无法在加1了？
    val result1 = atomic(0)
    val result2 = atomic(0)
    for (i in 1..TIMES) {
      val po = PromiseOut<Unit>()
      GlobalScope.launch {
        delay(100)
        result1.update { cur -> cur.inc() }
        po.resolve(Unit)
      }
      GlobalScope.launch {
        po.waitPromise()
        result2.update { cur -> cur.inc() }
      }
    }

    while (result2.value < TIMES) {
      delay(200)
      println("times result1:${result1.value} result2:${result2.value}")

    }
    assertEquals(result1.value, result2.value)
  }
}