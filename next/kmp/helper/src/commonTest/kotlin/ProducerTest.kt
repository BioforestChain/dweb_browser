package info.bagen.dwebbrowser

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.helper.OrderBy
import org.dweb_browser.helper.OrderInvoker
import org.dweb_browser.helper.Producer
import org.dweb_browser.helper.SafeLinkList
import org.dweb_browser.helper.addDebugTags
import org.dweb_browser.helper.await
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ProducerTest {
  init {
    addDebugTags(listOf("/.+/"))
  }
//  @Test
//  fun observeTest() = runCommonTest {
//    val producer = Producer<Int>(this)
//    producer.emit(1)
//    producer.emit(2)
//    producer.emit(3)
//    producer.emit(4)
//    delay(1000)
//    println("start consumer1")
//    var res1 = 0
//    producer.consumer {
//      it.onEach {
//        if (it.data <= 2) {
//          println("consumer1 ${it.data}")
//          res1 += it.data
//        }
//      }.onCompletion {
//        assertEquals(6, res1)
//      }
//    }
//    delay(1000)
////    launch {
////      var res1 = 0
////      consumer1.collect {
////        if (it.data <= 2) {
////          println("consumer1 ${it.data}")
////          res1 += it.data
////        }
////      }
////      assertEquals(6, res1)
////    }
//
//    producer.emit(1)
//    producer.emit(2)
//    producer.emit(3)
//    producer.emit(4)
//    delay(1000)
//    println("start consumer2")
//    val consumer2 = producer.consumer()
//    launch {
//      var res2 = 0
//      consumer2.collect {
//        if (it.data > 2) {
//          println("consumer2 ${it.data}")
//          res2 += it.data
//        }
//      }
//      assertEquals(14, res2)
//    }
//    delay(100)
//    producer.close()
//  }

  @Test
  fun consumeTest() = runCommonTest {
    val producer = Producer<Float>("test", this)
    producer.send(1f)
    producer.send(2f)
    producer.send(3f)
    producer.send(4f)
    delay(1000)
    println("start consumer1")
    val consumer1 = producer.consumer("consumer1")
    delay(1000)
    launch {
      var res1 = 0f
      consumer1.collect { event ->
        if (event.data <= 2.5f) {
          println("consumer1 ${event.data}")
          res1 += event.data
          event.consume()
          delay(500)
        }
      }
      println("assertEquals consumer1")
      assertEquals(6f, res1)
    }


    delay(2000)
    println("start consumer2")
    val consumer2 = producer.consumer("consumer2")
    launch {
      var res2 = 0f
      consumer2.collect { event ->
        println("consumer2 ${event.data}")
        res2 += event.data
        event.consume()
        delay(500)
      }
      println("assertEquals consumer2")
      assertEquals(14f, res2)
    }

    delay(1000)
    println("start emit more")
    producer.send(1f)
    producer.send(2f)
    producer.send(3f)
    producer.send(4f)
    producer.close()
  }

  @Test
  fun lazyTest() = runCommonTest {
    val producer = Producer<Float>("test", this)

    producer.send(1f)
    producer.send(2f)
    producer.send(3f)
    producer.send(4f)
    println("create consumer1")
    val consumer1 = producer.consumer("consumer1")
    producer.send(1f)
    producer.send(2f)
    producer.send(3f)
    producer.send(4f)

    println("start consumer1")
    delay(1000)
    launch {
      val res = atomic(0f)
      val job = consumer1.collectIn(this) { event ->
        event.consumeFilter {
          it <= 2f
        }?.also { value ->
          res.update { it + value }
          println("ADD $value")
        }
      }
      job.invokeOnCompletion {
        assertEquals(6f, res.value)
      }
    }


    delay(1000)
    producer.send(1f)
    producer.send(2f)
    producer.send(3f)
    producer.send(4f)

    producer.close()
  }

//  @Test
//  fun consumeTest() = runCommonTest {
//    val producer = Producer<Int>(this)
//    producer.emit(1)
//    producer.emit(2)
//    producer.emit(3)
//    producer.emit(4)
//    delay(1000)
//    println("start consumer1")
//    var res1 = 0
//    val consumer1 = producer.observe() {
//      it.onEach { event ->
//        if (event.data <= 2) {
//          println("consumer1 ${event.data}")
//          res1 += event.data
//          event.consume()
//          delay(500)
//        }
//      }.onCompletion {
//        assertEquals(6, res1)
//      }
//    }
//
//    delay(1000)
//    println("start consumer2")
//    var res2 = 0
//    producer.consumer {
//      it.onEach { event ->
//        println("consumer2 ${event.data}")
//        res2 += event.data
//        event.consume()
//        delay(500)
//      }.onCompletion {
//        assertEquals(14, res2)
//      }
//    }
//
//    delay(1000)
//    println("start emit more")
//    producer.emit(1)
//    producer.emit(2)
//    producer.close()
//  }

  @Test
  fun parallelEmit() = runCommonTest(30) {
    println("---test-$it")
    val producer = Producer<Int>("test", this)
    val res = SafeLinkList<Int>()
    val MAX = 100
    val DELAY = 500L
    val startTime = datetimeNow()
    producer.consumer("consumer1").collectIn(this) {
      res.add(it.consume())
      delay(DELAY)
      val diff = datetimeNow() - startTime
      println("diff=$diff data=${it.data} >> size=${res.size}")
      if (diff > DELAY * 2) {
        producer.close(Exception("over time: diff=$diff"))
      }

      if (res.size == MAX) {
        producer.close()
      }
    }
    val orderInvoker = OrderInvoker()
    for (i in 1..MAX) {
      launch(start = CoroutineStart.UNDISPATCHED) {
        orderInvoker.tryInvoke(null) {
          producer.send(i)
        }
      }
    }
    producer.await()
  }

  @Test
  fun orderEmit() = runCommonTest(10) {
    data class Data(val data: Int) : OrderBy {
      override val order = 1
    }

    val producer = Producer<Data>("test", this)
    val res = SafeLinkList<Data>()
    val MAX = 4
    val DELAY = 200L
    val startTime = datetimeNow()

    producer.consumer("consumer1").collectIn(this) {
      res.add(it.consume())
      delay(DELAY)
      println("time:${datetimeNow() - startTime} data=${it.data}")

      if (res.size == MAX) {
        val diff = datetimeNow() - startTime
        val total = MAX * DELAY
        if (diff >= total) {
          producer.close()
        } else {
          producer.close(Exception("wrong time: $diff < $total"))
        }
      }
    }
    val orderInvoker = OrderInvoker()
    for (i in 1..MAX) {
      launch(start = CoroutineStart.UNDISPATCHED) {
        orderInvoker.tryInvoke(null) {
          producer.send(Data(i))
        }
      }
    }
    producer.await()
  }

  @Test
  fun jobTest() = runCommonTest {
    run {
      val rootJob = Job()
      println("start")
      launch {
        delay(100)
        rootJob.cancel(CancellationException("qaq"))
      }
      rootJob.invokeOnCompletion {
        println(it)
      }
      var err: Throwable? = null
      try {
        rootJob.await()
      } catch (e: Throwable) {
        err = e
      }
      assertNull(err)
    }
    run {
      val rootJob = Job()
      println("start")
      launch {
        delay(100)
        rootJob.completeExceptionally(Exception("qzq"))
      }
      rootJob.invokeOnCompletion {
        println(it)
      }
      var err: Throwable? = null
      try {
        rootJob.await()
      } catch (e: Throwable) {
        err = e
      }
      assertNotNull(err)
    }
    run {
      val rootJob = Job()
      println("start")
      launch {
        delay(100)
        rootJob.cancel()
      }
      rootJob.invokeOnCompletion {
        println(it)
      }
      var err: Throwable? = null
      try {
        rootJob.await()
      } catch (e: Throwable) {
        err = e
      }
      assertNull(err)
    }
  }
}