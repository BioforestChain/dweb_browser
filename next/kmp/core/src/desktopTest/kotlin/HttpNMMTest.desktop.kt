package info.bagen.dwebbrowser

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.debug.DebugProbes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
class HttpNMMTestDesktop {
  val test = HttpNMMTest()

  init {
    DebugProbes.install()
  }

  @Test
  fun testWebSocket() = runCommonTest(100) {
    val testJob = launch {
      delay(5000)
      // Dump running coroutines
      DebugProbes.dumpCoroutines()
    }
    test.webSocketTester(this, it)
    testJob.cancel()
  }
}