package info.bagen.dwebbrowser

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.now
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertNotEquals

class FlowTest {

  @Test
  fun testAwaitEmit() = runCommonTest {
    val flow = MutableSharedFlow<Int?>()
    flow.filterNotNull().collectIn(this) {
      println("QWQ ${now()} collect1 got-start $it")
      delay(1000)
      println("QWQ ${now()} collect1 got-end $it")
    }
//    flow.collectIn(this) {
//      println("QWQ ${now()} collect2 got-start $it")
//      delay(1000)
//      println("QWQ ${now()} collect2 got-end $it")
//    }

    for (i in 0..10) {
      println("QWQ ${now()} emit-start $i")
      flow.emit(i)
      flow.emit(null)
      println("QWQ ${now()} emit-end $i")
    }

  }

  data class Event(val data: Int) {
    val emitLock = Mutex()
  }


  class ChannelFlow(val name: String) : Flow<Event> {
    val channel = Channel<Event>()
    override suspend fun collect(collector: FlowCollector<Event>) {
      coroutineScope {
        for (event in channel) {
          // 同一个事件的处理，不做任何阻塞，直接发出
          // 这里包一层launch，目的是确保不阻塞input的循环，从而确保上游event能快速涌入
          launch(start = CoroutineStart.UNDISPATCHED) {
            event.emitLock.lock()
            println("${now()} | $name start")
            collector.emit(event)
            println("${now()} | $name end")
            // 消息发送完成，开锁
            event.emitLock.unlock()
          }
        }
      }
    }
  }

  @Test
  fun testBackpressure() = runCommonTest {
    val flow1 = ChannelFlow("flow1")
    val flow2 = ChannelFlow("flow2")
    launch {
      flow1.collect {
        delay(1000)
        println("${now()} | ${flow1.name} ${it.data}")
        delay(1000)
      }
    }

    launch {
      flow2.collect {
        delay(1000)
        println("${now()} | ${flow2.name} ${it.data}")
        delay(1000)
      }
    }

    for (i in 1..10) {
      val event = Event(i)
      println("${now()} | ${flow1.name} send")
      flow1.channel.send(event)
      println("${now()} | ${flow2.name} send")
      flow2.channel.send(event)
      delay(2200)
    }
    flow1.channel.close()
    flow2.channel.close()
  }

  @Test
  fun testThrottle() = runCommonTest {
    val res = mutableListOf<Int>()
    val total = 400
    channelFlow {
      for (i in 1..total) {
        delay(10)
        send(i)
      }
      close()
    }.conflate().collect {
      println(it)
      res += it
      delay(100)
    }
    assertNotEquals(res.size, total)
  }

  @Test
  fun testCombine() = runCommonTest {
    val serverToClientChannel = Channel<String>()
    val clientToServerChannel = Channel<String>()
    val stateFlow = channelFlow {
      send(0)
      for (i in 0..300) {
        delay(10)
        send(i)
      }
      close()
      serverToClientChannel.send("end")// 直接发送结束帧
      serverToClientChannel.close()
      clientToServerChannel.close()
      println("end 1")
    }
    launch {
      var lastSentValue: Any? = null
      var canSend = false
      var frameAcc = 0
      // 同时处理 stateFlow 和 commandChannel
      stateFlow.combine(clientToServerChannel.receiveAsFlow().map {
        println("QWQ canSend=${it}")
        canSend = true
        frameAcc++
      }) { stateValue, frame ->
        Pair(stateValue, frame)
      }.collect { (stateValue) ->
        if (canSend && stateValue != lastSentValue) {
          println("QWQ do Send=${stateValue}")
          canSend = false
          lastSentValue = stateValue
          serverToClientChannel.send(stateValue.toString())
        }
      }
      println("end 2")
    }

    val result = channelFlow<String?> {
      clientToServerChannel.send("hi")
      for (msg in serverToClientChannel) {
        println("QWQ get $msg")
        delay(100)
        send(msg)
        send(null)
        println("QWQ do Get")
        if (!clientToServerChannel.isClosedForSend) {
          clientToServerChannel.send("get")
        }
      }
      println("end 3")
      close()
    }.filterNotNull()

    result.collect {
      println("QWQ collect $it")
      delay(100)
    }
  }

  @Test
  fun combineState() = runCommonTest {
    val stateFlow = MutableStateFlow(false)
    val sharedFlow = MutableSharedFlow<Int>(
      replay = 1,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val job = launch {
      sharedFlow.combine(stateFlow) { i, b ->
        i to b
      }.collect {
        println("QWQ $it")
      }
    }
//    delay(100)
    sharedFlow.emit(1)

//    delay(100)
    stateFlow.value = true

    //delay(100)
    stateFlow.value = false

    //delay(100)
    sharedFlow.emit(2)

    delay(100)
    job.cancel()
  }
}