package info.bagen.rust.plaoc.microService.core

import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.ipc.Ipc


interface BootstrapContext {
    val dns: DnsMicroModule
}

interface DnsMicroModule {
    /**
     * 动态安装应用
     */
    fun install(mm: MicroModule)

    /**
     * 动态卸载应用
     */
    fun uninstall(mm: MicroModule)

    /**
     * 与其它应用建立连接
     */
    suspend fun connect(mmid: Mmid): Ipc
}