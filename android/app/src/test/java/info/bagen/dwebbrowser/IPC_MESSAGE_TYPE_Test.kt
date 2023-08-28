package info.bagen.dwebbrowser

import org.dweb_browser.microservice.help.gson
import org.dweb_browser.microservice.ipc.helper.IPC_MESSAGE_TYPE
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class IPC_MESSAGE_TYPE_Test {
  @Test
  fun testGson() {
    val res = gson.toJson(IPC_MESSAGE_TYPE.REQUEST)
    assertEquals("0", res)
  }
}