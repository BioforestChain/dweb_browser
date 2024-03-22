package info.bagen.dwebbrowser

import kotlinx.coroutines.delay
import org.dweb_browser.helper.EventFlow
import org.dweb_browser.helper.SimpleEventFlow
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EventFlowTest {


  @Test
  fun testSuspendEmit() = runCommonTest {
    val eventFlow = SimpleEventFlow(this)
    var acc = 0
    println("testSuspendEmit start")
    eventFlow.listen {
      println("testSuspendEmit 1")
      delay(100)
      acc += 1
    }
    eventFlow.listen {
      println("testSuspendEmit 2")
      acc += 1
    }
    eventFlow.listen {
      println("testSuspendEmit 3")
      delay(3000)
      acc += 1
    }
    eventFlow.emitAndClear()
    println("testSuspendEmit emit $acc")
    assertEquals(acc, 3)
  }

  @Test
  fun testMuteEmit() = runCommonTest {
    val eventFlow = EventFlow<Int>(this)
    var acc = 0
    eventFlow.listen {
      println("testMuteEmit listen $it")
      acc = 1 + it
    }
    println("testMuteEmit1 $acc")
    eventFlow.emit(acc)
    println("testMuteEmit2 $acc")
    eventFlow.emitAndClear(acc)
    println("testMuteEmit3 $acc")
    assertEquals(acc, 2)
  }
}