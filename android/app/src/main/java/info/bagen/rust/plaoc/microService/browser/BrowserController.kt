package info.bagen.rust.plaoc.microService.browser

import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.webview.DWebView

class BrowserController(
    val mmid: Mmid,
    val localeMM: MicroModule,
    val remoteMM: MicroModule,
) {

    data class BrowserItem(
        val browserId: String,
//        val browser: BrowserActivity,
    ) {

    }

    fun openApp(mmid: Mmid): BrowserItem {
        return BrowserItem("1")
    }

}