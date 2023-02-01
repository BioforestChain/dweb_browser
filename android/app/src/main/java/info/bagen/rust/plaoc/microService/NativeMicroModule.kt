package info.bagen.rust.plaoc.microService

import info.bagen.rust.plaoc.openHomeActivity
import info.bagen.rust.plaoc.system.callable_map

/** 启动Boot服务*/
fun startBootNMM() {
    BootNMM().bootstrap(WindowOptions(processId=null))
}

open class NativeMicroModule(override val mmid: Mmid = "sys.dweb") : MicroModule() {
    override fun bootstrap(args:WindowOptions) {
        println("Kotlin#NativeMicroModule mmid:$mmid bootstrap $args")
        dns_map[mmid]?.let { it -> it(args) }
    }

    override fun ipc(): Ipc {
        TODO("Not yet implemented")
    }
}


abstract class MicroModule {
    open val mmid: String = ""
    abstract fun bootstrap(args:WindowOptions)
    abstract fun ipc(): Ipc
}