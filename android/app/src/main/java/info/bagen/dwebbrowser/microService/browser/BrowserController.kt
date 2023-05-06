package info.bagen.dwebbrowser.microService.browser

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.JsonSyntaxException
import info.bagen.dwebbrowser.microService.helper.*
import info.bagen.dwebbrowser.microService.ipc.Ipc
import info.bagen.dwebbrowser.microService.ipc.IpcEvent
import info.bagen.dwebbrowser.microService.sys.dns.nativeFetch
import info.bagen.dwebbrowser.microService.sys.jmm.JmmMetadata
import info.bagen.dwebbrowser.network.HttpClient
import info.bagen.dwebbrowser.network.base.byteBufferToString
import info.bagen.dwebbrowser.ui.browser.ios.BrowserViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.http4k.core.Uri
import org.http4k.core.query

class BrowserController(val browserNMM: BrowserNMM) {
    val showLoading: MutableState<Boolean> = mutableStateOf(false)
    val browserViewModel by lazy { BrowserViewModel(this) }

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

    val currentInsets : MutableState<WindowInsetsCompat> by lazy {
        mutableStateOf(WindowInsetsCompat.toWindowInsetsCompat(
            activity!!.window.decorView.rootWindowInsets
        ))
    }
    // mutableStateOf(WindowInsetsCompat.toWindowInsetsCompat(activity!!.window.decorView.rootWindowInsets))

    @Composable
    fun effect(activity: BrowserActivity): BrowserController {
        /**
         * 这个 NativeUI 的逻辑是工作在全屏幕下，所以会使得默认覆盖 系统 UI
         */
        SideEffect {
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            /// system-bar 一旦隐藏（visible = false），那么被手势划出来后，过一会儿自动回去
            //windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            ViewCompat.setOnApplyWindowInsetsListener(activity.window.decorView) { _, insets ->
                currentInsets.value = insets
                Log.e("lin.huang", "InsetsListener -> ${insets.getInsets(WindowInsetsCompat.Type.ime())}")
                Log.e("lin.huang", "InsetsListener -> ${insets.getInsets(WindowInsetsCompat.Type.statusBars())}")
                insets
            }

        }
        return this
    }

    private val openIPCMap = mutableMapOf<Mmid, Ipc>()
    private val dWebViewList = mutableListOf<View>()

    fun appendView(view: View) = dWebViewList.add(view)
    val hasDwebView get() = dWebViewList.size > 0

    fun removeLastView(): Boolean {
        try {
            dWebViewList.removeLast().also { childView ->
                activity?.window?.decorView?.let { parentView ->
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
            val (ipc) = browserNMM.connect(mmid)
            ipc.onEvent {
                if (it.event.name == EIpcEvent.Ready.event) { // webview加载完成，可以隐藏加载框
                    BrowserNMM.browserController.showLoading.value = false
                    debugBrowser("openApp", "event::${it.event.name}==>${it.event.data}  from==> $mmid ")
                }
            }
            ipc
        }.also { ipc ->
            debugBrowser("openApp", "postMessage==>activity  $mmid")
            ipc.postMessage(IpcEvent.fromUtf8(EIpcEvent.Activity.event, ""))
        }
    }

    private suspend fun installJMM(jmmMetadata: JmmMetadata, url: String) = browserNMM.nativeFetch(
        Uri.of("file://jmm.sys.dweb/install")
            .query("mmid", jmmMetadata.id).query("metadataUrl", url)
    )

    suspend fun uninstallJMM(jmmMetadata: JmmMetadata) = browserNMM.nativeFetch(
        Uri.of("file://jmm.sys.dweb/uninstall").query("mmid", jmmMetadata.id)
    )

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