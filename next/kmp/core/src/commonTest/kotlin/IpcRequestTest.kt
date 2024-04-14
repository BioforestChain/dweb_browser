package info.bagen.dwebbrowser

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
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
import org.dweb_browser.helper.addDebugTags
import org.dweb_browser.helper.collectIn
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals

class IpcRequestTest {
  init {
    addDebugTags(listOf("/.+/"))
  }

  // 黑盒测试ipcRequest
  @Test
  fun testIpcRequestInBlackBox() = runCommonTest {
    val channel = NativeMessageChannel(kotlinIpcPool.scope, "from.id.dweb", "to.id.dweb")
    val fromMM = TestMicroModule()
    val toMM = TestMicroModule()
    val pid = 0
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

    class TestMicroModule(mmid: String = "test.ipcChannel.dweb") :
      NativeMicroModule(mmid, "test IpcChannel") {
      inner class TestRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
        override suspend fun _bootstrap() {
          routes(
            //
            "/channel" byChannel { ctx ->
              for (msg in ctx.income) {
                ctx.sendText((msg.text.toInt() * 2).toString())
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

    /// 用来测试前面发起的ws不会阻塞后面的请求
    val job1 = CompletableDeferred<Unit>()
    val job2 = CompletableDeferred<Unit>()

    launch(start = CoroutineStart.UNDISPATCHED) {
      var actual = 0
      var expected = 0
      clientRuntime.createChannel("file://${serverMM.mmid}/channel") {
        job1.complete(Unit)
        job2.await()
        launch {
          for (i in 1..10) {
            actual += i * 2
            sendText("$i")
          }
          delay(1000)
          close()
        }

        for (frame in income) {
          println("client got msg: $frame")
          expected += frame.text.toInt()
        }
      }
      assertEquals(expected = expected, actual = actual)
    }

    launch(start = CoroutineStart.UNDISPATCHED) {
      var actual = 0
      var expected = 0
      job1.await()
      clientRuntime.createChannel("file://${serverMM.mmid}/channel") {
        launch {
          for (i in 1..10) {
            actual += i * 2
            sendText("$i")
          }
          delay(1000)
          close()
        }

        for (frame in income) {
          println("client got msg: $frame")
          expected += frame.text.toInt()
        }
      }
      assertEquals(expected = expected, actual = actual)
      job2.complete(Unit)
    }
  }
}