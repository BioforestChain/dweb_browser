package info.bagen.rust.plaoc.microService.sys.boot

import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.core.Router
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.helper.toURLQueryComponent
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.http4k.core.Method
import org.http4k.routing.bind
import org.http4k.routing.routes


inline fun debugBoot(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("boot", tag, msg, err)

class BootNMM(initMmids: List<Mmid>? = null) : NativeMicroModule("boot.sys.dweb") {
    /**
     * 开机启动项注册表
     * TODO 这里需要从数据库中读取
     */
    private val registeredMmids = mutableSetOf<Mmid>()

    init {
        if (initMmids != null) {
            registeredMmids += initMmids
        }
    }

    override val routers: Router = mutableMapOf()
    override suspend fun _bootstrap() {
        apiRouting = routes(
            "/register" bind Method.GET to defineHandler { _, ipc ->
                register(ipc.remote.mmid)
            },
            "/unregister" bind Method.GET to defineHandler { _, ipc ->
                unregister(ipc.remote.mmid)
            }
        )

        GlobalScope.launch {
            for (mmid in registeredMmids) {
                debugBoot("launch", mmid)
                nativeFetch("file://dns.sys.dweb/open?app_id=${mmid.toURLQueryComponent()}")
            }
        }
    }

    override suspend fun _shutdown() {
        routers.clear()
    }

    /**
     * 注册一个boot程序
     * TODO 这里应该有用户授权，允许开机启动
     */
    private fun register(mmid: Mmid) = this.registeredMmids.add(mmid);

    /**
     * 移除一个boot程序
     * TODO 这里应该有用户授权，取消开机启动
     */
    private fun unregister(mmid: Mmid) = this.registeredMmids.remove(mmid);
}
