package info.bagen.rust.plaoc.microService.browser

import android.content.Intent
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.microService.ipc.IpcEvent
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewController
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewNMM


class BrowserNMM : NativeMicroModule("browser.sys.dweb") {
    companion object {
        var activityPo: PromiseOut<BrowserActivity>? = null
        val browserControllerMap = mutableMapOf<Mmid, BrowserController>()
    }

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        val activity = PromiseOut<BrowserActivity>().also {
            activityPo = it
            App.startActivity(BrowserActivity::class.java) { intent ->
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
        }.waitPromise()
        _afterShutdownSignal.listen { activity.finish() }


    }

    suspend fun openApp(mmid: Mmid): String {
        val (ipc) = bootstrapContext.dns.connect("file://$mmid")
        ipc.postMessage(IpcEvent.fromUtf8("activity", ""))
        val remoteMmid = ipc.remote.mmid
        val remoteMm = ipc.asRemoteInstance()
            ?: throw Exception("browser.sys.dweb/open should be call by locale")
        val controller = browserControllerMap.getOrPut(remoteMmid) {
            BrowserController(
                remoteMmid,
                this,
                remoteMm,
            )
        }
        return controller.openApp(mmid).browserId
    }

    override suspend fun _shutdown() {
    }
}