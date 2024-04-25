package info.bagen.dwebbrowser

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.now
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test

class FlowTest {

  @Test
  fun testAwaitEmit() = runCommonTest {
    val flow = MutableSharedFlow<Int?>()
    flow.filterNotNull().collectIn(this) {
      println("QAQ ${now()} collect1 got-start $it")
      delay(1000)
      println("QAQ ${now()} collect1 got-end $it")
    }
//    flow.collectIn(this) {
//      println("QAQ ${now()} collect2 got-start $it")
//      delay(1000)
//      println("QAQ ${now()} collect2 got-end $it")
//    }

    for (i in 0..10) {
      println("QAQ ${now()} emit-start $i")
      flow.emit(i)
      flow.emit(null)
      println("QAQ ${now()} emit-end $i")
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
}