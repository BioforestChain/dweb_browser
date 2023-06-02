package info.bagen.dwebbrowser.microService.core

import info.bagen.dwebbrowser.microService.helper.AdapterManager
import info.bagen.dwebbrowser.microService.core.ipc.Ipc
import org.http4k.core.Request

/**
 * 两个模块的连接结果：
 *
 * 1. fromIpc 是肯定有的，这个对象是我们当前的上下文发起连接得来的通道，要与 toMM 通讯都需要通过它
 * 1. toIpc 则不一定，远程模块可能是自己创建了 Ipc，我们的上下文拿不到这个内存对象
 */
data class ConnectResult(val ipcForFromMM: info.bagen.dwebbrowser.microService.core.ipc.Ipc, val ipcForToMM: info.bagen.dwebbrowser.microService.core.ipc.Ipc?) {
    val component1 get() = ipcForFromMM
    val component2 get() = ipcForToMM
}

typealias ConnectAdapter = suspend (fromMM: MicroModule, toMM: MicroModule, reason: Request) -> ConnectResult?

val connectAdapterManager = AdapterManager<ConnectAdapter>()


/** 外部程序与内部程序建立链接的方法 */
suspend fun connectMicroModules(
  fromMM: MicroModule, toMM: MicroModule, reason: Request
): ConnectResult {
    for (connectAdapter in connectAdapterManager.adapters) {
        val ipc = connectAdapter(fromMM, toMM, reason)
        if (ipc != null) {
            return ipc
        }
    }
    throw Exception("no support connect MicroModules, from:${fromMM.mmid} to:${toMM.mmid}")
}