package info.bagen.rust.plaoc.microService.browser

import android.content.Intent
import android.os.Bundle
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.IpcEvent

fun debugBrowser(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("browser", tag, msg, err)

class BrowserNMM() : NativeMicroModule("browser.sys.dweb") {
    companion object {
        lateinit var browserController: BrowserController
    }
    init {
       browserController = BrowserController("browser.sys.dweb", this)
    }

    private val openIPCMap = mutableMapOf<Mmid, Ipc>()

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    }

    suspend fun openApp(mmid: Mmid) {
        openIPCMap.getOrPut(mmid) {
            val (ipc) = bootstrapContext.dns.connect(mmid)
            ipc.onEvent {
                if (it.event.name == "ready") { // 说法加载完成，可以隐藏加载框
                    browserController.showLoading.value = false
                    debugBrowser("openApp", "event::${it.event.name}==>${it.event.data}")
                }
            }
            ipc
        }.also { ipc ->
            debugBrowser("openApp", "postMessage==>activity")
            ipc.postMessage(IpcEvent.fromUtf8("activity", ""))
        }
    }

    suspend fun closeApp(webViewId: String) {
        val (ipc) = bootstrapContext.dns.connect(mmid)
        debugBrowser("closeApp", "webViewId = $webViewId")
        ipc.postMessage(IpcEvent.fromUtf8("close", webViewId))
    }

    suspend fun closeAllApp() {
        val (ipc) = bootstrapContext.dns.connect(mmid)
        debugBrowser("closeAllApp", "closeAllApp")
        ipc.postMessage(IpcEvent.fromUtf8("closeAll", ""))
    }

    override suspend fun _shutdown() {
    }

    fun openBrowserActivity() {
        App.startActivity(BrowserActivity::class.java) { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            intent.putExtras(Bundle().also { b -> b.putString("mmid", mmid) })
        }
    }
}