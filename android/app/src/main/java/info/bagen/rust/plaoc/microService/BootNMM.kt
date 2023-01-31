package info.bagen.rust.plaoc.microService

typealias Router = MutableMap<String, IO>
typealias IO = (mmid: Any) -> Any

class BootNMM : NativeMicroModule() {
    private val registeredMmids = mutableSetOf<Mmid>()
    private val routers: Router = mutableMapOf()

    init {
        // 初始化注册微组件的函数
        routers["register"] = put@{ mmid  ->
            return@put registerMicro(mmid as Mmid)
        }
        routers["unregister"] = put@{ mmid ->
            return@put unRegisterMicro(mmid as Mmid)
        }
        // 初始化启动一个dwebView
        registeredMmids.add("boot.sys.dweb")
    }

    override fun bootstrap() {
        // 初始化启动一个dwebView --(new MicroModule.from('desktop.sys.dweb') as JsMicroModule).boostrap()
        for (mmid in registeredMmids) {
            NativeMicroModule(mmid).bootstrap()
        }
    }

    private fun registerMicro(mmid: Mmid): Boolean {

        return registeredMmids.add(mmid)
    }

    private fun unRegisterMicro(mmid: Mmid): Boolean {

        return registeredMmids.remove(mmid)
    }

//    $Routers:{
//        '/register': IO<mmid, boolean>
//        '/unregister': IO<mmid, boolean>
//    }

}
