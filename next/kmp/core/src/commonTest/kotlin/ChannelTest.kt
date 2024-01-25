package info.bagen.dwebbrowser

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test

class ChannelTest {
  @Test
  fun selectingToSend() = runCommonTest {
    fun CoroutineScope.produceNumbers(side: SendChannel<Int>) = produce<Int> {
      for (num in 1..10) { // produce 10 numbers from 1 to 10
        delay(100) // every 100 ms
        select<Unit> {
          onSend(num) {} // Send to the primary channel
          side.onSend(num) {} // or to the side channel
        }
      }
    }

    val side = Channel<Int>() // allocate side channel
    this.launch { // this is a very fast consumer for the side channel
      side.consumeEach { println("Side channel has $it") }
    }
    produceNumbers(side).consumeEach {
      println("Consuming $it")
      delay(250) // let us digest the consumed number properly, do not hurry
    }
    println("Done consuming")
    coroutineContext.cancelChildren()
  }
}