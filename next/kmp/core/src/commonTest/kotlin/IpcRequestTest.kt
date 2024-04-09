package info.bagen.dwebbrowser

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.core.ipc.IpcRequestInit
import org.dweb_browser.core.ipc.NativeMessageChannel
import org.dweb_browser.core.ipc.helper.IpcResponse
import org.dweb_browser.core.ipc.kotlinIpcPool
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.createChannel
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.helper.collectIn
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureBinaryFrame
import org.dweb_browser.pure.http.PureCloseFrame
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureTextFrame
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
    val pid = kotlinIpcPool.generatePid()
    val senderIpc = kotlinIpcPool.createIpc(channel.port1, pid, fromMM, toMM)
    val receiverIpc = kotlinIpcPool.createIpc(channel.port2, pid, toMM, fromMM)

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
      receiverIpc.onRequest("test").collectIn(this) { event ->
        val request = event.consume()
        val data = request.body.toString()
        println("receiverIpc结果🧨=> $data ")
        assertEquals("senderIpc 除夕快乐", data)
        receiverIpc.postMessage(
          IpcResponse.fromText(
            request.reqId, text = "receiverIpc 除夕快乐", ipc = receiverIpc
          )
        )
      }
      senderIpc.onRequest("test").onEach { event ->
        val request = event.consume()
        val data = request.body.text()
        println("senderIpc结果🧨=> $data ${senderIpc.remote.mmid}")
        assertEquals("senderIpc", data)
        senderIpc.postMessage(
          IpcResponse.fromText(
            request.reqId, text = "senderIpc 除夕快乐", ipc = senderIpc
          )
        )
      }.launchIn(this)
    }
  }

  @Test
  fun testIpcChannel() = runCommonTest {

    class TestMicroModule(mmid: String = "test.ipcPool.dweb") :
      NativeMicroModule(mmid, "test IpcPool") {
      inner class TestRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
        override suspend fun _bootstrap() {
          routes(
            //
            "/channel" byChannel { ctx ->
              for (msg in ctx.income) {
                when (msg) {
                  PureCloseFrame -> {}
                  is PureBinaryFrame -> ctx.sendText("echo: ${msg.data.decodeToString()}")
                  is PureTextFrame -> ctx.sendText("echo: ${msg.data}")
                }
              }
            },
          )
        }

        override suspend fun _shutdown() {
        }
      }

      override fun createRuntime(bootstrapContext: BootstrapContext) = TestRuntime(bootstrapContext)

    }

    val dns = DnsNMM()
    val serverMM = TestMicroModule("server.mm.dweb")
    val clientMM = TestMicroModule("client.mm.dweb")
    dns.install(clientMM)
    dns.install(serverMM)
    val dnsRuntime = dns.bootstrap()
    val clientRuntime = dnsRuntime.open(clientMM.mmid) as NativeMicroModule.NativeRuntime;

    val clientIpc = clientRuntime.connect(serverMM.mmid)
    clientRuntime.createChannel("file://${serverMM.mmid}/channel") {
      launch {
        for (i in 1..10) {
          sendText("hi~$i")
        }
        delay(1000)
        close()
      }

      for (i in income) {
        println(i)
      }
    }
  }
}