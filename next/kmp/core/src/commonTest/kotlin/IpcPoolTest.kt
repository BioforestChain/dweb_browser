package info.bagen.dwebbrowser

import org.dweb_browser.core.ipc.IpcOptions
import org.dweb_browser.core.ipc.NativeIpc
import org.dweb_browser.core.ipc.NativeMessageChannel
import org.dweb_browser.core.ipc.helper.IPC_STATE
import org.dweb_browser.core.ipc.helper.IpcPoolPack
import org.dweb_browser.core.ipc.kotlinIpcPool
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertEquals


class TestMicroModule(mmid: String = "test.ipcPool.dweb") :
  NativeMicroModule(mmid, "test IpcPool") {
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    TODO("Not yet implemented")
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }

}

class IpcPoolTest {
  @Test  // 测试基础通信生命周期的建立
  fun testCreateNativeIpc() = runCommonTest {
    val fromMM = TestMicroModule("from.mm.dweb")
    val toMM = TestMicroModule("to.mm.dweb")
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
    println("2\uD83E\uDDE8=> ${fromNativeIpc.channelId} ${toNativeIpc.channelId}")
    toNativeIpc.onLifeCycle {
      println("toNativeIpc\uD83E\uDDE8=> ${it.event.state}")
    }
    fromNativeIpc.onLifeCycle {
      println("fromNativeIpc\uD83E\uDDE8=> ${it.event.state}")
    }
    assertEquals(fromNativeIpc.awaitStart().state, IPC_STATE.OPEN)
    assertEquals(toNativeIpc.awaitStart().state, IPC_STATE.OPEN)
    fromMM.shutdown()
    toMM.shutdown()
  }
}