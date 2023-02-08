package info.bagen.rust.plaoc.microService

import info.bagen.rust.plaoc.openHomeActivity

class BootNMM : NativeMicroModule() {
    override val mmid: Mmid = "boot.sys.dweb"
    private val routers: Router = mutableMapOf()
    private val hookBootApp = mutableMapOf<Mmid,AppRun>()
    private val registeredMmids = mutableSetOf<Mmid>()

    init {
        // 注册路由
        routers["/open"] = put@{
            return@put open(it)
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

    override fun bootstrap(routerTarget:String, options: NativeOptions): Any? {
        println("kotlin#BootNMM bootstrap==> $options")
        // 导航到自己的路由
        if (!routers.containsKey(routerTarget)) {
           return "boot.sys.dweb route not found for $routerTarget"
        }
        return routers[routerTarget]?.let { it -> it(options) }
    }

    fun open(options: NativeOptions):Any {
        val origin = options["origin"]
        println("kotlinBootNMM start app:$origin,${hookBootApp.containsKey(origin)}")
        if (origin == null) {
            return "Error not Found param origin"
        }
        // 如果已经注册了该boot app
        if (hookBootApp.containsKey(origin)) {
          return (hookBootApp[origin]!!)(options) // 直接调用该方法
        }
        return "Error not register $origin boot Application"
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
