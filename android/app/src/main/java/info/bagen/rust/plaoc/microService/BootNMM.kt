package info.bagen.rust.plaoc.microService

import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.openHomeActivity
import org.http4k.routing.RoutingHttpHandler

class BootNMM(override var routes: RoutingHttpHandler?) : NativeMicroModule("boot.sys.dweb") {
    override val routers: Router = mutableMapOf()
    override fun _bootstrap() {
        initBootApp()
        // 初始化启动一个桌面系统程序
        registeredMmids.add("desktop.bfs.dweb")
    }

    override fun _shutdown() {
        routers.clear()
    }

    private val hookBootApp = mutableMapOf<Mmid,AppRun>()
    private val registeredMmids = mutableSetOf<Mmid>()

    // 打开一个boot程序
    fun open(origin:String,options:NativeOptions):Any {
        // 如果已经注册了该boot app
        if (hookBootApp.containsKey(origin)) {
          return (hookBootApp[origin]!!)(options) // 直接调用该方法
        }
        return "Error not register $origin boot Application"
    }
    // 注册一个boot程序
    private fun registerMicro(mmid: Mmid): Boolean {
        return registeredMmids.add(mmid)
    }
    // 移除一个boot程序
    private fun unRegisterMicro(mmid: Mmid): Boolean {
       return registeredMmids.remove(mmid)
    }

    private fun initBootApp() {
        hookBootApp["desktop.bfs.dweb"] = {
            openHomeActivity()
        }
    }
}
