package info.bagen.rust.plaoc.microService

import info.bagen.rust.plaoc.openHomeActivity


typealias Router = MutableMap<String, IO>
typealias IO = (mmid: Any) -> Any
typealias AppRun = (options: NativeOptions) -> Any


class BootNMM : NativeMicroModule() {
    override val mmid: Mmid = "boot.sys.dweb"
    private val routers: Router = mutableMapOf()
    private val hookBootApp = mutableMapOf<Mmid,AppRun>()
    private val registeredMmids = mutableSetOf<Mmid>()

    init {
        // 注册路由
        routers["/open"] = put@{
            return@put open(it as NativeOptions)
        }
        // 初始化注册微组件的函数
        routers["/register"] = put@{ mmid ->
            return@put registerMicro(mmid as Mmid)
        }
        routers["/unregister"] = put@{ mmid ->
            return@put unRegisterMicro(mmid as Mmid)
        }
        initBootApp()
        // 初始化启动一个桌面系统程序
        registeredMmids.add("desktop.bfs.dweb")
    }

    override fun bootstrap(args: workerOption): Any? {
        println("kotlin#BootNMM bootstrap==> ${args.mainCode}  ${args.origin}")
        // 导航到自己的路由
        if (routers[args.routerTarget] == null) {
           return "boot.sys.dweb route not found for ${args.routerTarget}"
        }
        return routers[args.routerTarget]?.let { it -> it(args) }
    }

    fun open(options: NativeOptions):Any {
        println("kotlinBootNMM start app:${options.origin},${hookBootApp.containsKey(options.origin)},${options.routerTarget}")
        // 如果已经注册了该boot app
        if (hookBootApp.containsKey(options.origin)) {
          return (hookBootApp[options.origin]!!)(options) // 直接调用该方法
        }
        return "Error not register ${options.origin} boot Application"
    }


    private fun registerMicro(mmid: Mmid): Boolean {
        return registeredMmids.add(mmid)
    }

    private fun unRegisterMicro(mmid: Mmid): Boolean {
       return registeredMmids.remove(mmid)
    }

    private fun initBootApp() {
        hookBootApp["desktop.bfs.dweb"] = {
            openHomeActivity()
        }
    }
}
