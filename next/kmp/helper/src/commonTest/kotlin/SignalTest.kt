package info.bagen.dwebbrowser

import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test

class SignalTest {

  val signal = SimpleSignal()

  @Test
  fun testOnMessage() = runCommonTest {
    val onClose = signal.toListener().also {
      it.invoke {
        println("it=> ${signal.size}")
      }
    }
    onClose {
      println("it onClose1")
    }
    signal.emit()
    onClose {
      println("it onClose2")
    }
  }
}