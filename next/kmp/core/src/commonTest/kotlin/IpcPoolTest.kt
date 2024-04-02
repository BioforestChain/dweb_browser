package info.bagen.dwebbrowser

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.ipc.NativeMessageChannel
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcResponse
import org.dweb_browser.core.ipc.helper.LIFECYCLE_STATE
import org.dweb_browser.core.ipc.kotlinIpcPool
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.boot.BootNMM
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.helper.addDebugTags
import org.dweb_browser.helper.collectIn
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals


class TestMicroModule(mmid: String = "test.ipcPool.dweb") :
  NativeMicroModule(mmid, "test IpcPool") {
  inner class TestRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    override suspend fun _bootstrap() {
      routes("/test" bind PureMethod.GET by defineEmptyResponse {
        println("请求到了 /test")
        ipc.onRequest.onEach { request ->
          val pathName = request.uri.encodedPath
          println("/test 拿到结果=> $pathName")
          ipc.postMessage(
            IpcResponse.fromText(
              request.reqId, 200, PureHeaders(), "返回结果", ipc
            )
          )
        }.launchIn(mmScope)
        ipc.awaitOpen()
      })
    }

    override suspend fun _shutdown() {
    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = TestRuntime(bootstrapContext)

}

class IpcPoolTest {
  init {
    addDebugTags(listOf("/.+/"))
  }

  @Test  // 测试基础通信生命周期的建立
  fun testCreateNativeIpc() = runCommonTest {
    val fromMM = TestMicroModule("from.mm.dweb")
    val toMM = TestMicroModule("to.mm.dweb")
    val channel = NativeMessageChannel(kotlinIpcPool.scope, fromMM.id, toMM.id)
    println("1🧨=> ${fromMM.mmid} ${toMM.mmid}")
    val pid = kotlinIpcPool.generatePid()
    val fromNativeIpc = kotlinIpcPool.createIpc(channel.port1, pid, fromMM, toMM)
    val toNativeIpc = kotlinIpcPool.createIpc(channel.port2, pid, toMM, fromMM)
    toNativeIpc.onEvent.collectIn(this@runCommonTest) { event ->
      println("🌞 toNativeIpc $event")
      assertEquals(event.text, "xx")
    }
    println("🌞📸 send")
    fromNativeIpc.postMessage(IpcEvent.fromUtf8("哈哈", "xx"))
    assertEquals(fromNativeIpc.awaitOpen().state, LIFECYCLE_STATE.OPENED)
    assertEquals(toNativeIpc.awaitOpen().state, LIFECYCLE_STATE.OPENED)
  }

  @Test
  fun testCreateReadableStreamIpc() = runCommonTest {
    val dns = DnsNMM()
    val serverMM = TestMicroModule("server.mm.dweb")
    val clientMM = TestMicroModule("client.mm.dweb")
    dns.install(
      BootNMM(
        listOf(
          clientMM.mmid, serverMM.mmid
        )
      )
    )
    dns.install(clientMM)
    dns.install(serverMM)
    dns.bootstrap()
    val clientRuntime = dns.runtime.open(clientMM.mmid);
    val testIpc = clientRuntime.connect(serverMM.mmid).fork()
    testIpc.start()
    testIpc.request("file://request.mm.dweb/test")
    val res = testIpc.request("https://test.com/test")
    val data = res.body.toPureString()
    println("👾 $data")
    assertEquals(data, "返回结果")

    dns.runtime.shutdown()
  }
}