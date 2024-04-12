package info.bagen.dwebbrowser

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test

class FlowTest {

  @Test
  fun testAwaitEmit()  = runCommonTest{
    val flow = MutableSharedFlow<Unit>()
    launch {
      println("xxxx emit 1")
      flow.emit(Unit)
      println("xxxx emit end")
    }

   val job1 = launch {
      flow.collect {

      }
    }
    val job2 = launch {
      flow.collect {

      }
    }


  }
}