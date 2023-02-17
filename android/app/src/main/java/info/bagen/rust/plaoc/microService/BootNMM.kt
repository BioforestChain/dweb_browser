package info.bagen.rust.plaoc.microService

import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.toURLQueryComponent
import info.bagen.rust.plaoc.microService.network.nativeFetch
import info.bagen.rust.plaoc.openHomeActivity
import kotlinx.coroutines.runBlocking
import org.http4k.core.Method
import org.http4k.routing.bind
import org.http4k.routing.routes
import kotlin.collections.set

class BootNMM : NativeMicroModule("boot.sys.dweb") {
    override val routers: Router = mutableMapOf()
    override fun _bootstrap() {
        initBootApp()
        registeredMmids.add("desktop.bfs.dweb")

        apiRouting = routes(
            "/register" bind Method.GET to defineHandler { _, ipc ->
                register(ipc.remote.mmid)
            },
            "/unregister" bind Method.GET to defineHandler { _, ipc ->
                unregister(ipc.remote.mmid)
            }
        )

        runBlocking {
            for (mmid in registeredMmids) {
                nativeFetch("file://dns.sys.dweb/open=${mmid.toURLQueryComponent()}")
            }
        }

    }

    override fun _shutdown() {
        routers.clear()
    }

    private val hookBootApp = mutableMapOf<Mmid, AppRun>()

    /**
     * TODO 这里需要从数据库中读取
     */
    private val registeredMmids = mutableSetOf<Mmid>(
        // 初始化启动一个桌面系统程序
        "desktop.bfs.dweb"
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


    private fun initBootApp() {
        hookBootApp["desktop.bfs.dweb"] = {
            openHomeActivity()
        }
    }
}
