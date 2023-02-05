package info.bagen.rust.plaoc.microService


typealias Router = MutableMap<String, IO>
typealias IO = (mmid: Any) -> Any


class BootNMM : NativeMicroModule() {
    override val mmid: Mmid = "boot.sys.dweb"
    private val routers: Router = mutableMapOf()
    private val registeredMmids = mutableMapOf<Mmid, MicroModule>()

    init {
        // 初始化注册微组件的函数
        routers["register"] = put@{ mmid ->
            return@put registerMicro(mmid as Mmid)
        }
        routers["unregister"] = put@{ mmid ->
            return@put unRegisterMicro(mmid as Mmid)
        }
        // 初始化启动一个桌面系统程序
        registeredMmids["desktop.bfs.dweb"] = NativeMicroModule("desktop.bfs.dweb")
    }

    override fun bootstrap(args: NativeOptions) {
        registeredMmids.values.forEach { microModule ->
            println("Kotlin#BootNMM:$microModule")
            microModule.bootstrap(args)
        }
    }

    private fun registerMicro(mmid: Mmid): Boolean {
        val micro = registeredMmids.put(mmid, NativeMicroModule(mmid))
        if (micro !== null) {
            return true
        }
        return false
    }

    private fun unRegisterMicro(mmid: Mmid): Boolean {
        val micro = registeredMmids.remove(mmid)
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
