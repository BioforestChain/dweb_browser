package info.bagen.rust.plaoc.microService

import info.bagen.rust.plaoc.openHomeActivity


typealias Router = MutableMap<String, IO>
typealias IO = (mmid: Any) -> Any
typealias AppRun = (options: NativeOptions) -> Any


class BootNMM : NativeMicroModule() {
    override val mmid: Mmid = "boot.sys.dweb"
    val routers: Router = mutableMapOf()
    private val bootApp = mutableMapOf<Mmid,AppRun>()

    init {
        // 注册路由
        routers["/open"] = put@{
            return@put open(it as NativeOptions)
        }
        // 初始化注册微组件的函数
        routers["/register"] = put@{ mmid ->
//            return@put registerMicro(mmid as Mmid)
        }
        routers["/unregister"] = put@{ mmid ->
//            return@put unRegisterMicro(mmid as Mmid)
        }
        // 初始化启动一个桌面系统程序
        bootApp["desktop.bfs.dweb"] = {
            openHomeActivity()
        }
    }

    fun open(options: NativeOptions):Any {
        println("kotlinBootNMM start app:${options.origin},${bootApp.containsKey(options.origin)},${options.routerTarget}")
        // 如果已经注册了该boot app
        if (bootApp.containsKey(options.origin)) {
          return (bootApp[options.origin]!!)(options) // 直接调用该方法
        }
        return "Error not register ${options.origin} boot Application"
    }


    private fun registerMicro(mmid: Mmid,app:AppRun): Boolean {
        val micro = bootApp.put(mmid, app)
        if (micro !== null) {
            return true
        }
        return false
    }

    private fun unRegisterMicro(mmid: Mmid,app:AppRun): Boolean {
        val micro = bootApp.remove(mmid)
        if (micro !== null) {
            return true
        }
        return false
    }

//    $Routers:{
//        '/register': IO<mmid, boolean>
//        '/unregister': IO<mmid, boolean>
//    }

}
