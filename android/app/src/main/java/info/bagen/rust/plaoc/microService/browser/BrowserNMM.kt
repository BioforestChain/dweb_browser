package info.bagen.rust.plaoc.microService.browser

import android.content.Intent
import android.os.Bundle
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.IpcEvent

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

    override suspend fun onActivity(event: IpcEvent, ipc: Ipc) {
        App.startActivity(BrowserActivity::class.java) { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            intent.putExtras(Bundle().also { b -> b.putString("mmid", mmid) })
        }
    }

    override suspend fun _shutdown() {
    }
}