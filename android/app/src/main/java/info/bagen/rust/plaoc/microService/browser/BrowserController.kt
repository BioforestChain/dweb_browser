package info.bagen.rust.plaoc.microService.browser

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import info.bagen.rust.plaoc.App
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
    val mmid: Mmid,
    val localeMM: BrowserNMM,
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

    val showLoading: MutableState<Boolean> = mutableStateOf(false)

    val addViewList = mutableListOf<View>()
    fun appendView(view: View) {
        addViewList.add(view)
    }
    fun removeLastView() : Boolean {
        try {
            addViewList.removeLast().also { childView ->
                App.browserActivity?.window?.decorView?.let { parentView ->
                    (parentView as ViewGroup).removeView(childView)
                }
            }
        } catch (e: NoSuchElementException) {
            return false
        }
        return true
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