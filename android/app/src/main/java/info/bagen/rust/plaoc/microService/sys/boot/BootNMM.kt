package info.bagen.rust.plaoc.microService.sys.boot

import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.core.Router
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.toURLQueryComponent
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.http4k.core.Method
import org.http4k.routing.bind
import org.http4k.routing.routes

class BootNMM : NativeMicroModule("boot.sys.dweb") {
    override val routers: Router = mutableMapOf()
    override suspend fun _bootstrap() {
        apiRouting = routes(
            "/open" bind Method.GET to defineHandler { request ->
                println("BootNMM#apiRouting===>$mmid  ${request.uri.path}")
                true
            },
            "/register" bind Method.GET to defineHandler { _, ipc ->
                register(ipc.remote.mmid)
            },
            "/unregister" bind Method.GET to defineHandler { _, ipc ->
                unregister(ipc.remote.mmid)
            }
        )

        GlobalScope.launch {
            for (mmid in registeredMmids) {
                nativeFetch("file://dns.sys.dweb/open?app_id=${mmid.toURLQueryComponent()}")
            }
        }

    }

    override suspend fun _shutdown() {
        routers.clear()
    }

    /**
     * 开机启动项注册表
     * TODO 这里需要从数据库中读取
     */
    private val registeredMmids = mutableSetOf<Mmid>(
        // 初始化启动一个桌面系统程序
        "desktop.user.dweb"
    )

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
