package info.bagen.rust.plaoc.microService.browser

import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.printdebugln

fun debugBrowser(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("browser", tag, msg, err)

class BrowserNMM : NativeMicroModule("browser.sys.dweb") {
    companion object {
        lateinit var browserController: BrowserController
    }

    init {
       browserController = BrowserController("browser.sys.dweb", this)
    }

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    }

    override suspend fun _shutdown() {
    }
}