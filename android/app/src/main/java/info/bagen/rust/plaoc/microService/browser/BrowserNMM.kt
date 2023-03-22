package info.bagen.rust.plaoc.microService.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.ipc.IpcEvent

fun debugBrowser(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("browser", tag, msg, err)

class BrowserNMM : NativeMicroModule("browser.sys.dweb") {
    companion object {
        private val browserNMM: BrowserNMM by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            BrowserNMM()
        }

        val browserController by lazy {
            BrowserController("browser.sys.dweb", browserNMM)
        }
    }

    @SuppressLint("SuspiciousIndentation")
    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        openBrowserActivity()
    }

    suspend fun openApp(mmid: Mmid) {
        val (ipc) = bootstrapContext.dns.connect(mmid)
        ipc.postMessage(IpcEvent.fromUtf8("activity", ""))
        ipc.onEvent {
            if (it.event.name == "ready") { // 说法加载完成，可以隐藏加载框
                browserController.showLoading.value = false
            }
        }
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