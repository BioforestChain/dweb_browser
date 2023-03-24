package info.bagen.rust.plaoc.microService.browser

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.google.gson.JsonSyntaxException
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.helper.*
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.IpcEvent
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.network.HttpClient
import info.bagen.rust.plaoc.network.base.byteBufferToString
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.http4k.core.Uri
import org.http4k.core.query

class BrowserController(val mmid: Mmid, val localeMM: BrowserNMM) {
    val showLoading: MutableState<Boolean> = mutableStateOf(false)

    private val openIPCMap = mutableMapOf<Mmid, Ipc>()
    private val dWebViewList = mutableListOf<View>()

    fun appendView(view: View) = dWebViewList.add(view)
    val hasDwebView get() = dWebViewList.size > 0

    fun removeLastView(): Boolean {
        try {
            dWebViewList.removeLast().also { childView ->
                App.browserActivity?.window?.decorView?.let { parentView ->
                    (parentView as ViewGroup).removeView(childView)
                }
            }
        } catch (e: NoSuchElementException) {
            return false
        }
        return true
    }

    suspend fun openApp(mmid: Mmid) {
        openIPCMap.getOrPut(mmid) {
            val (ipc) = localeMM.bootstrapContext.dns.connect(mmid)
            ipc.onEvent {
                if (it.event.name == "ready") { // 说法加载完成，可以隐藏加载框
                    BrowserNMM.browserController.showLoading.value = false
                    debugBrowser("openApp", "event::${it.event.name}==>${it.event.data}")
                }
            }
            ipc
        }.also { ipc ->
            debugBrowser("openApp", "postMessage==>activity")
            ipc.postMessage(IpcEvent.fromUtf8("activity", ""))
        }
    }

    private suspend fun installJMM(jmmMetadata: JmmMetadata, url: String) = localeMM.nativeFetch(
        Uri.of("file://jmm.sys.dweb/install")
            .query("mmid", jmmMetadata.id).query("metadataUrl", url)
    )

    fun openBrowserActivity() {
        App.startActivity(BrowserActivity::class.java) { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            intent.putExtras(Bundle().also { b -> b.putString("mmid", mmid) })
        }
    }

    fun checkJmmMetadataJson(url: String): Boolean {
        android.net.Uri.parse(url).lastPathSegment?.let { lastPathSegment ->
            if (lastPathSegment.endsWith(".json")) { // 如果是json，进行请求判断并解析jmmMetadata
                try {
                    val jmmMetadata = gson.fromJson(
                        byteBufferToString(HttpClient().requestPath(url).body.payload),
                        JmmMetadata::class.java
                    )
                    GlobalScope.launch(ioAsyncExceptionHandler) {
                        installJMM(jmmMetadata, url)
                    }
                    return true
                } catch (e: JsonSyntaxException) {
                    Log.e("DWebBrowserModel", "checkJmmMetadataJson fail -> ${e.message}")
                }
            }
        }
        return false
    }
}