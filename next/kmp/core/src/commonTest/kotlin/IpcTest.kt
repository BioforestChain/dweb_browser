package info.bagen.dwebbrowser

import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test

class IpcTest {

  @Test
  fun IpcEventTest() = runCommonTest {
    var orderBy = 0
    while (orderBy < 10) {
      orderBy = Ipc.order_by_acc++
      println("orderBy=> $orderBy")
    }
  }
}