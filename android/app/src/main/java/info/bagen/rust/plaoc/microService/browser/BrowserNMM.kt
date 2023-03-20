package info.bagen.rust.plaoc.microService.browser

import android.content.Intent
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.microService.helper.gson
import info.bagen.rust.plaoc.microService.ipc.IpcEvent
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.microService.sys.jmm.JmmNMM.Companion.nativeFetchInstallApp
import org.http4k.core.Uri
import org.http4k.core.query


class BrowserNMM : NativeMicroModule("browser.sys.dweb") {
    companion object {
        var activityPo: PromiseOut<BrowserActivity>? = null

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

    suspend fun openApp(mmid: Mmid) {
        val (ipc) = bootstrapContext.dns.connect("file://$mmid")
        ipc.postMessage(IpcEvent.fromUtf8("activity", ""))
    }

    override suspend fun _shutdown() {
    }
}