package info.bagen.rust.plaoc.microService.browser

import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.microService.helper.ioAsyncExceptionHandler
import info.bagen.rust.plaoc.microService.helper.runBlockingCatching
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import org.http4k.core.Uri
import org.http4k.core.query
import java.util.concurrent.atomic.AtomicInteger

class BrowserController(
    open val mmid: Mmid,
    open val localeMM: BrowserNMM,
) {
    companion object {
        private var browserId_acc = AtomicInteger(1)
    }

    private var activityTask = PromiseOut<BrowserActivity>()
    suspend fun waitActivityCreated() = activityTask.waitPromise()
    var activity: BrowserActivity? = null
        set(value) {
            if (field == value) {
                return
            }
            field = value
            if (value == null) {
                activityTask = PromiseOut()
            } else {
                activityTask.resolve(value)
            }
        }

    data class BrowserItem(
        val browserId: String,
        //val browser: BrowserActivity?,
    )

    fun createApp(): BrowserItem {
        return BrowserItem("#browser${browserId_acc.getAndAdd(1)}")
    }

    suspend fun openApp(mmid: Mmid) = localeMM.openApp(mmid)

    suspend fun installJMM(jmmMetadata: JmmMetadata, url: String) = localeMM.nativeFetch(
        Uri.of("file://jmm.sys.dweb/install")
            .query("mmid", jmmMetadata.id).query("metadataUrl", url)
    )

    fun openBrowserActivity() = localeMM.openBrowserActivity()
}