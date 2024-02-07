package info.bagen.dwebbrowser

import org.dweb_browser.core.ipc.helper.IPC_MESSAGE_TYPE
import org.dweb_browser.helper.JsonLoose
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test

class CryptoTest {
  @Test
  fun testRegex() = runCommonTest {
    val typeInfo =
      Regex(""""type"\s*:\s*(\d+)""").find("""{"type":7,"name":"pong","data":"","encoding":2,"orderBy":null}""")
    println("data=> $typeInfo")
    val icpMessage = JsonLoose.decodeFromString<IPC_MESSAGE_TYPE>(typeInfo!!.groupValues[1])
    println("icpMessage=> $icpMessage")
  }

}