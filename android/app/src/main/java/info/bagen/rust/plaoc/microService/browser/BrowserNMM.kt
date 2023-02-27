package info.bagen.rust.plaoc.microService.browser

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.NativeMicroModule


class BrowserNMM : NativeMicroModule("browser.sys.dweb") {
    override suspend fun _bootstrap() {
        val intent = Intent(App.appContext.applicationContext, BrowserActivity::class.java).apply {
            addFlags(FLAG_ACTIVITY_NEW_TASK)
        }
        App.appContext.startActivity(intent)
    }

    override suspend fun _shutdown() {
        BrowserActivity.instance?.finish()
    }
}