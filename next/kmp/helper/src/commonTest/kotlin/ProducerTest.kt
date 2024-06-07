package info.bagen.dwebbrowser

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.dweb_browser.helper.OrderBy
import org.dweb_browser.helper.Producer
import org.dweb_browser.helper.SafeFloat
import org.dweb_browser.helper.SafeHashSet
import org.dweb_browser.helper.SafeLinkList
import org.dweb_browser.helper.addDebugTags
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.helper.now
import org.dweb_browser.helper.rand
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
      val res = SafeFloat(0f)
      val job = consumer1.collectIn(this) { event ->
        event.consumeFilter {
          it <= 2f
        }?.also { value ->
          res += value
          println("ADD $value=$res")
        }
      }
      job.invokeOnCompletion {
        println("ADD-DONE")
        assertEquals(9f, res.value)
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
  fun parallelEmit() = runCommonTest(10) {
    println("---test-$it")
    val producer = Producer<Int>("test", this)
    val res = SafeLinkList<Int>()
    val MAX = 30
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
    for (i in 1..MAX) {
      launch(start = CoroutineStart.UNDISPATCHED) {
        producer.send(i)
      }
    }
    producer.join()
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
    for (i in 1..MAX) {
      launch(start = CoroutineStart.UNDISPATCHED) {
        producer.send(Data(i))
      }
    }
    producer.join()
  }

  @Test
  fun closeTest() = runCommonTest(1000) {
    println("---test-$it")

    data class Data(val data: Int) : OrderBy {
      override val order = 1
    }

    val MAX = 2

    val producer = Producer<Data>("test", this)
    val res = SafeLinkList<Data>()

    producer.consumer("consumer1").collectIn(this) {
      res.add(it.consume())
      println("${now()} $it")
    }
    for (i in 1..MAX) {
      producer.send(Data(i))
    }
    producer.closeAndJoin()

    assertEquals(MAX, res.size)
  }

  @Test
  fun testMultiConsumer() = runCommonTest {
    addDebugTags(listOf("Producer<test>"))
    data class Data(val data: Int) : OrderBy {
      override val order = 1
    }

    val MAX = 20
    val producer = Producer<Data>("test", this)
    val res = SafeLinkList<Data>()

    producer.consumer("consumer1").collectIn(this) {
      res.add(it.consume())
      println("${now()} $it ${res.size}")
    }
    producer.consumer("consumer2").collectIn(this) {
      res.add(it.consume())
      println("${now()} $it ${res.size}")
    }
    for (i in 1..MAX) {
      producer.send(Data(i))
    }
    producer.closeAndJoin()

    assertEquals(MAX * 2, res.size)
  }

  @Test
  fun testStopImmediatePropagation() = runCommonTest {
    data class Data(val data: Int) : OrderBy {
      override val order = 1
    }

    val MAX = 20
    val producer = Producer<Data>("test", this)
    val res = SafeLinkList<Data>()

    producer.consumer("consumer1").collectIn(this) {
      res.add(it.consume())
      println("${now()} $it")
    }
    producer.consumer("consumer2").collectIn(this) {
      res.add(it.consume())
      println("${now()} $it")
      it.stopImmediatePropagation()
    }
    producer.consumer("consumer3").collectIn(this) {
      res.add(it.consume())
      println("${now()} $it")
    }
    for (i in 1..MAX) {
      producer.send(Data(i))
    }
    producer.closeAndJoin()

    assertEquals(MAX, res.size / 2)
  }

  @Test
  fun testCancel() = runCommonTest {
    val parentScope = globalDefaultScope
    val producer = Producer<Unit>("test", parentScope)
    val DEALY = 100L
    launch {
      delay(DEALY)
      parentScope.cancel()
    }
    val start = datetimeNow()
    producer.join()
    val end = datetimeNow()
    assertTrue(end - start >= DEALY)
  }

  @Test
  fun testConsumerFirst() = runCommonTest {
    data class Data(val data: Int) : OrderBy {
      override val order = 1
    }

    coroutineScope {

      val producer = Producer<Data>("test", this)

      val actualDeferred = async { producer.consumer("consumer").map { it.consume() }.first().data }

      val expected = 123456
      producer.send(Data(expected))
      val actual = actualDeferred.await()
      println("QWQ actual=$actual")
      assertEquals(expected, actual)

      producer.close()
    }
  }

  @Test
  fun testConsumerFirst2() = runCommonTest {
    data class Data(val data: Int) : OrderBy {
      override val order = 1
    }

    val MAX = 10
    val producer = Producer<Data>("test", this)
    val res = SafeLinkList<Deferred<Data>>()

    for (i in 1..MAX) {
      val task = async(start = CoroutineStart.UNDISPATCHED) {
        producer.consumer("consumer$i").map { it.stopImmediatePropagation(); it.consume(); }.first()
      }
      res.add(task)
    }

    val expected = mutableListOf<Int>()
    for (i in 1..MAX) {
      producer.send(Data(i))
      expected.add(i)
    }

    val actual = res.map { it.await().data }
    println("QWQ expected=$expected")
    println("QWQ actual=$actual")
    assertContentEquals(expected, actual)
    producer.close()
  }

  @Test
  fun testParentClose() = runCommonTest {
    val parentScope = CoroutineScope(SupervisorJob())
    val producer = Producer<Int>("test", parentScope)
    producer.consumer("blocker").collectIn(this) {
      delay(100000)
      println(it.consume())
    }
    producer.send(123)
    launch {
      delay(3000)
      parentScope.cancel()
    }

    val startTime = datetimeNow()
    val deferred = CompletableDeferred<Unit>()
    producer.invokeOnClose {
      deferred.complete(Unit)
    }
    deferred.await()
    val endTime = datetimeNow()
    assertTrue(endTime - startTime < 4000)
  }

  @Test
  fun testDoubleEmitBug() = runCommonTest(1000) { time ->
    println("---test-$time")
    val parentScope = CoroutineScope(SupervisorJob())

    val producer = Producer<Int>("test", parentScope)
    val size = 20;
    producer.warningThreshold = size
    val res = SafeHashSet<Int>()
    val jobs = mutableListOf<Job>()
    for (j in 1..rand(0, 10)) {
      jobs += launch {
        producer.consumer("test-before-$j").collectIn(this@runCommonTest) {
          delay(rand(0, 10).toLong())
        }
      }
    }

    for (i in 1..size) {
      jobs += launch {
//        if (i > size / 2) {
//          delay(10)
//        }
        producer.send(i)
      }
    }
    val done = CompletableDeferred<Unit>()
    jobs += launch {
      producer.consumer("test").collectIn(this@runCommonTest) {
        if (res.isEmpty()) {
          println("${now()} | QWQ start first")
        }
        val data = it.consume()
        if (res.contains(it.data)) {
          assertTrue("QWQ double emit!!") { false }
        }
        res.add(data)
        if (res.size == size) {
          println("${now()} | QWQ got $size")
          done.complete(Unit)
        }
      }
    }
    for (j in 1..rand(0, 10)) {
      jobs += launch {
        producer.consumer("test-after-$j").collectIn(this@runCommonTest) {
          delay(rand(0, 10).toLong())
        }
      }
    }
    jobs.joinAll()
    println("${now()} | QWQ send end")
    done.await()
    producer.closeAndJoin()
    assertEquals(size, res.size)
  }
}