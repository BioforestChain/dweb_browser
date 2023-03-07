package info.bagen.rust.plaoc.microService.core

import info.bagen.rust.plaoc.microService.helper.AdapterManager
import info.bagen.rust.plaoc.microService.ipc.Ipc
import org.http4k.core.Request


typealias ConnectAdapter = suspend (fromMM: MicroModule, toMM: MicroModule, reason: Request) -> Ipc?

val connectAdapterManager = AdapterManager<ConnectAdapter>()


/** 外部程序与内部程序建立链接的方法 */
suspend fun connectMicroModules(fromMM: MicroModule, toMM: MicroModule, reason: Request): Ipc {
    for (connectAdapter in connectAdapterManager.adapters) {
        val ipc = connectAdapter(fromMM, toMM, reason)
        if (ipc != null) {
            return ipc
        }
    }
    throw Exception("no support connect MicroModules, from:${fromMM.mmid} to:${toMM.mmid}")
}