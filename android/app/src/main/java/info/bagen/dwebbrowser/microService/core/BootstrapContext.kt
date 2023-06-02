package info.bagen.dwebbrowser.microService.core

import info.bagen.dwebbrowser.microService.helper.Mmid
import org.http4k.core.Request


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
     * 动态js应用查询
     */
    fun query(mmid: Mmid): MicroModule?

    /**
     * 重启应用
     */
    fun restart(mmid:Mmid)

    /**
     * 与其它应用建立连接
     */
    suspend fun connect(mmid: Mmid, reason: Request? = null): ConnectResult

    /**
     * 启动其它应用
     */
    suspend fun bootstrap(mmid: Mmid)
}