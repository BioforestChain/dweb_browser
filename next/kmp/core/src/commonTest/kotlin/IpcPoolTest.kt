package info.bagen.dwebbrowser

import kotlinx.coroutines.launch
import org.dweb_browser.core.ipc.IpcOptions
import org.dweb_browser.core.ipc.NativeIpc
import org.dweb_browser.core.ipc.NativeMessageChannel
import org.dweb_browser.core.ipc.helper.IPC_STATE
import org.dweb_browser.core.ipc.helper.IpcLifeCycle
import org.dweb_browser.core.ipc.helper.IpcPoolPack
import org.dweb_browser.core.ipc.kotlinIpcPool
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals


class TestMicroModule : NativeMicroModule("test.ipcPool.dweb", "test IpcPool") {
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    TODO("Not yet implemented")
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }

}

class IpcPoolTest {
  val mm = TestMicroModule()

  @Test
  fun `test IpcPool create MessagePort`() = runCommonTest {
    this.launch {
//      val channel = IDWebView.create(mm, DWebViewOptions(privateNet = true))
//      val port1 = channel.port1
//      val port2 = channel.port2
//      val ipcClient =
//        kotlinIpcPool.create(
//          Endpoint.Worker,
//          IpcOptions("create-process-client", mm, port = port1)
//        )
//      ipcClient.postMessage(IpcLifeCycle(IPC_STATE.OPEN))
//      val ipcServer =
//        kotlinIpcPool.create(
//          Endpoint.Worker,
//          IpcOptions("create-process-client", mm, port = port2)
//        )
//      ipcServer.onLifeCycle {
//        assertEquals(it.event.state, IPC_STATE.OPEN)
//      }
    }
  }

  @Test
  fun `test IpcPool create NativePort`() = runCommonTest {
    val fromMM = TestMicroModule()
    val toMM = TestMicroModule()
    val channel = NativeMessageChannel<IpcPoolPack, IpcPoolPack>(fromMM.id, toMM.id)
    println("1🧨=> ${fromMM.mmid} ${toMM.mmid}")
    val fromNativeIpc = kotlinIpcPool.create<NativeIpc>(
      "from-native",
      IpcOptions(toMM, channel = channel.port1)
    )
    val toNativeIpc = kotlinIpcPool.create<NativeIpc>(
      "to-native",
      IpcOptions(fromMM, channel = channel.port2)
    )
//    fromNativeIpc.postMessage(IpcLifeCycle(IPC_STATE.OPEN))
    println("2\uD83E\uDDE8=> ${fromNativeIpc.channelId} ${toNativeIpc.channelId}")
    toNativeIpc.onLifeCycle {
      println("3\uD83E\uDDE8=> ${it.event.state}")
      assertEquals(it.event.state, IPC_STATE.OPEN)
    }
  }
}