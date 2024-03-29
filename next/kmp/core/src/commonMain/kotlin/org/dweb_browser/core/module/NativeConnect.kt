package org.dweb_browser.core.module

import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.help.AdapterManager
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.pure.http.PureRequest

/**
 * 两个模块的连接结果：
 *
 * 1. fromIpc 是肯定有的，这个对象是我们当前的上下文发起连接得来的通道，要与 toMM 通讯都需要通过它
 * 1. toIpc 则不一定，远程模块可能是自己创建了 Ipc，我们的上下文拿不到这个内存对象
 */
class ConnectResult(val ipcForFromMM: Ipc, val ipcForToMM: Ipc) {
  operator fun component1() = ipcForFromMM
  operator fun component2() = ipcForToMM
}

typealias ConnectAdapter = suspend (fromMM: MicroModule, toMM: MicroModule.Runtime, reason: PureRequest) -> Ipc?

val connectAdapterManager = AdapterManager<ConnectAdapter>()


/** 外部程序与内部程序建立链接的方法 */
suspend fun connectMicroModules(
  fromMM: MicroModule, toMM: MicroModule.Runtime, reason: PureRequest
): Ipc {
  for (connectAdapter in connectAdapterManager.adapters) {
    connectAdapter(fromMM, toMM, reason)?.also { return it }
  }
  throw Exception("no support connect MicroModules, from:${fromMM.mmid} to:${toMM.mmid}")
}


internal var grant: CompletableDeferred<Boolean>? = null

/**
 * 启动拦截器，确保前置任务完成后，才会开始运行microModule
 */
fun NativeMicroModule.Companion.interceptStartApp(granter: CompletableDeferred<Boolean>) {
  grant = granter
}