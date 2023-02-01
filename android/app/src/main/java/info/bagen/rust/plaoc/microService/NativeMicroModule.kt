package info.bagen.rust.plaoc.microService

/** 启动Boot服务*/
fun startBootNMM() {
    BootNMM().bootstrap(BootOptions())
}

open class NativeMicroModule(override val mmid: Mmid = "sys.dweb") : MicroModule() {
    override fun bootstrap(args:BootOptions) {
        println("Kotlin#NativeMicroModule mmid:$mmid bootstrap $args")
        dns_map[mmid]?.let { it -> it(args) }
    }

    override fun ipc(): Ipc {
        TODO("Not yet implemented")
    }
}


abstract class MicroModule {
    open val mmid: String = ""
    abstract fun bootstrap(args:BootOptions)
    abstract fun ipc(): Ipc
}