package info.bagen.rust.plaoc.microService

import info.bagen.rust.plaoc.openHomeActivity

class BootNMM : NativeMicroModule("boot.sys.dweb") {
    override val routers: Router = mutableMapOf()
    override fun _bootstrap() {
        // 注册路由
        routers["/open"] = put@{ options->
            val origin = options["origin"]
            println("kotlinBootNMM start app:$origin,${hookBootApp.containsKey(origin)}")
            if (origin == null) {
                return@put "Error not Found param origin"
            }
            return@put open(origin,options)
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
