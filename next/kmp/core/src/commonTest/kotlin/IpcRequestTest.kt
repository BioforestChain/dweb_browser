package info.bagen.dwebbrowser

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.dweb_browser.core.ipc.IpcRequestInit
import org.dweb_browser.core.ipc.NativeMessageChannel
import org.dweb_browser.core.ipc.helper.IpcResponse
import org.dweb_browser.core.ipc.kotlinIpcPool
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals

class IpcRequestTest {

  // 黑盒测试ipcRequest
  @Test
  fun testIpcRequestInBlackBox() = runCommonTest {
    val channel = NativeMessageChannel(kotlinIpcPool.scope, "from.id.dweb", "to.id.dweb")
    val fromMM = TestMicroModule()
    val toMM = TestMicroModule()
    val senderIpc = kotlinIpcPool.createIpc(channel.port1, fromMM, toMM)
    val receiverIpc = kotlinIpcPool.createIpc(channel.port2, toMM, fromMM)

    launch {
      // send text body
      println("🧨=> send text body")
      val response = senderIpc.request(
        "https://test.dwebdapp_1.com", IpcRequestInit(
          method = PureMethod.POST, body = IPureBody.from("senderIpc")
        )
      )
      val res = response.body.text()
      println("🧨=> ${response.statusCode} $res")
      assertEquals(res, "senderIpc 除夕快乐")
    }
    launch {
      println("🧨=>  开始监听消息")
      receiverIpc.onRequest.onEach { request ->
        val data = request.body.toString()
        println("receiverIpc结果🧨=> $data ")
        assertEquals("senderIpc 除夕快乐", data)
        receiverIpc.postMessage(
          IpcResponse.fromText(
            request.reqId, text = "receiverIpc 除夕快乐", ipc = receiverIpc
          )
        )
      }.launchIn(this)
      senderIpc.onRequest.onEach { request ->
        val data = request.body.text()
        println("senderIpc结果🧨=> $data ${senderIpc.remote.mmid}")
        assertEquals("senderIpc", data)
        senderIpc.postMessage(
          IpcResponse.fromText(
            request.reqId,
            text = "senderIpc 除夕快乐",
            ipc = senderIpc
          )
        )
      }.launchIn(this)
    }
  }

  @Test
  fun testIpcRequest() = runCommonTest {

  }
}