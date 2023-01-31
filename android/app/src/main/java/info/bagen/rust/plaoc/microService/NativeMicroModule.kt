package info.bagen.rust.plaoc.microService

open class NativeMicroModule(override val mmid: Mmid = "sys.dweb") : MicroModule() {
    override fun bootstrap() {
        TODO("Not yet implemented")
    }

    override fun ipc(): Ipc {
        TODO("Not yet implemented")
    }
}


abstract class MicroModule {
    open val mmid: String = ""
    abstract fun bootstrap()
    abstract fun ipc(): Ipc
}