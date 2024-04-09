package info.bagen.dwebbrowser

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Producer
import org.dweb_browser.helper.addDebugTags
import org.dweb_browser.helper.collectIn
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
    producer.emit(1f)
    producer.emit(2f)
    producer.emit(3f)
    producer.emit(4f)
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
    producer.emit(1f)
    producer.emit(2f)
    producer.emit(3f)
    producer.emit(4f)
    producer.close()
  }

  @Test
  fun lazyTest() = runCommonTest {
    val producer = Producer<Float>("test", this)

    producer.emit(1f)
    producer.emit(2f)
    producer.emit(3f)
    producer.emit(4f)
    println("create consumer1")
    val consumer1 = producer.consumer("consumer1")
    producer.emit(1f)
    producer.emit(2f)
    producer.emit(3f)
    producer.emit(4f)

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
    producer.emit(1f)
    producer.emit(2f)
    producer.emit(3f)
    producer.emit(4f)

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
}