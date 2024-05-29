package info.bagen.dwebbrowser

import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals

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

  @Test
  fun testListenAndOff() = runCommonTest {
    val onClose = signal.toListener()
    for (i in 0..10000) {
      val off = onClose {

      }
      assertEquals(onClose.signal.size, 1)
      off()
      assertEquals(onClose.signal.size, 0)
    }
  }
}