package info.bagen.rust.plaoc.microService.core


interface BootstrapContext {
    val dns: DnsMicroModule
}

interface DnsMicroModule {
    fun install(mm: MicroModule)
    fun uninstall(mm: MicroModule)
}