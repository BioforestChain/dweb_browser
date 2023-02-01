package info.bagen.rust.plaoc.microService


typealias Router = MutableMap<String, IO>
typealias IO = (mmid: Any) -> Any

class BootNMM : NativeMicroModule() {
    private val routers: Router = mutableMapOf()
    private val registeredMmids = DwebDNS()
    init {
        // 初始化注册微组件的函数
        routers["register"] = put@{ mmid  ->
            return@put registerMicro(mmid as Mmid)
        }
        routers["unregister"] = put@{ mmid ->
            return@put unRegisterMicro(mmid as Mmid)
        }
        // 初始化启动一个桌面系统程序
        registeredMmids.add("desktop.sys.dweb",NativeMicroModule("desktop.sys.dweb"))
    }

    override fun bootstrap(args: WindowOptions) {
        // 初始化启动一个dwebView --(new MicroModule.from('desktop.sys.dweb') as JsMicroModule).boostrap()
        registeredMmids.dnsTables.values.forEach { microModule ->
            println("Kotlin#BootNMM:$microModule")
            microModule.bootstrap(args)
        }
    }

    private fun registerMicro(mmid: Mmid): Boolean {

        return registeredMmids.add(mmid,NativeMicroModule(mmid))
    }

    private fun unRegisterMicro(mmid: Mmid): String {
        return registeredMmids.remove(mmid)
    }

//    $Routers:{
//        '/register': IO<mmid, boolean>
//        '/unregister': IO<mmid, boolean>
//    }

}
