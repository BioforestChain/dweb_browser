package info.bagen.rust.plaoc.microService.browser

import android.content.Intent
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.PromiseOut


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

    override suspend fun _shutdown() {
    }
}