package info.bagen.dwebbrowser

import io.ktor.http.URLBuilder
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.ipc.NativeMessageChannel
import org.dweb_browser.core.ipc.helper.LIFECYCLE_STATE
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcResponse
import org.dweb_browser.core.ipc.kotlinIpcPool
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.boot.BootNMM
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.buildUnsafeString
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureStreamBody
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals


class TestMicroModule(mmid: String = "test.ipcPool.dweb") :
  NativeMicroModule(mmid, "test IpcPool") {
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      "/test" bind PureMethod.POST by definePureStreamHandler {
        println("请求到了 /test")
        val streamIpc = kotlinIpcPool.createIpc(
          "TestMicroModule/test",
          ipc.remote,
          request.body.toPureStream(),
        )
        println("xxx=> ${streamIpc.isActivity}")
        streamIpc.onRequest.onEach { (request, ipc) ->
          val pathName = request.uri.encodedPath
          println("/test 拿到结果=> $pathName")
          ipc.postMessage(IpcResponse.fromText(request.reqId, 200, PureHeaders(), "返回结果", ipc))
        }.launchIn(ioAsyncScope)
        streamIpc.input.stream
      })
  }

  override suspend fun _shutdown() {
  }

}

class IpcPoolTest {
  @Test  // 测试基础通信生命周期的建立
  fun testCreateNativeIpc() = runCommonTest {
    val fromMM = TestMicroModule("from.mm.dweb")
    val toMM = TestMicroModule("to.mm.dweb")
    val channel = NativeMessageChannel(kotlinIpcPool.scope, fromMM.id, toMM.id)
    println("1🧨=> ${fromMM.mmid} ${toMM.mmid}")
    val fromNativeIpc = kotlinIpcPool.createIpc(
      "from-native",
      toMM,
      channel.port1
    )
    val toNativeIpc = kotlinIpcPool.createIpc(
      "to-native",
      fromMM,
      channel.port2
    )
    toNativeIpc.eventFlow.onEach { (event) ->
      println("🌞 toNativeIpc $event")
      assertEquals(event.text, "xx")
    }.launchIn(this)
    println("🌞📸 send")
    fromNativeIpc.postMessage(IpcEvent.fromUtf8("哈哈", "xx"))
    assertEquals(fromNativeIpc.awaitStart().state, LIFECYCLE_STATE.OPENED)
    assertEquals(toNativeIpc.awaitStart().state, LIFECYCLE_STATE.OPENED)
    fromMM.shutdown()
    toMM.shutdown()
  }

  @Test
  fun testCreateReadableStreamIpc() = runCommonTest {
    val dns = DnsNMM()
    val streamMM = TestMicroModule("stream.mm.dweb")
    val requestMM = TestMicroModule("request.mm.dweb")
    dns.install(
      BootNMM(
        listOf(
          requestMM.mmid,
          streamMM.mmid
        )
      )
    )
    dns.install(requestMM)
    dns.install(streamMM)
    dns.bootstrap()
    val streamIpc = kotlinIpcPool.createIpc(
      "test-stream",
      streamMM,
    ) {
      streamMM.nativeFetch(
        PureClientRequest(
          URLBuilder("file://request.mm.dweb/test").apply {
          }.buildUnsafeString(),
          PureMethod.POST,
          body = PureStreamBody(it.input.stream)
        )
      ).stream()
    }
    val res = streamIpc.request("https://test.com/test")
    val data = res.body.toPureString()
    println("👾 $data")
    assertEquals(data, "返回结果")

    streamMM.shutdown()
    requestMM.shutdown()
  }
}