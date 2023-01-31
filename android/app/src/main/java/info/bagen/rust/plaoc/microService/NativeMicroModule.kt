package info.bagen.rust.plaoc.microService

import info.bagen.rust.plaoc.openHomeActivity

/** 启动Boot服务*/
fun startBootNMM() {
    BootNMM().bootstrap(WindowOptions(processId=null))
}

open class NativeMicroModule(override val mmid: Mmid = "sys.dweb") : MicroModule() {
    override fun bootstrap(args:WindowOptions) {
        println("Kotlin#NativeMicroModule bootstrap $args")
        openHomeActivity()
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