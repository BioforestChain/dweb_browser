package info.bagen.rust.plaoc.microService.sys.mwebview

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateMapOf
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.AndroidNativeMicroModule
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.ioAsyncExceptionHandler
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.ipc.Ipc
import io.ktor.util.collections.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.util.concurrent.ConcurrentSkipListSet

inline fun debugMultiWebView(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("mwebview", tag, msg, err)

class MultiWebViewNMM : AndroidNativeMicroModule("mwebview.sys.dweb") {
    class ActivityClass(var mmid: Mmid, val ctor: Class<out MultiWebViewActivity>)

    companion object {
        val activityClassList = mutableListOf(
            ActivityClass("", MultiWebViewPlaceholder1Activity::class.java),
            ActivityClass("", MultiWebViewPlaceholder2Activity::class.java),
            ActivityClass("", MultiWebViewPlaceholder3Activity::class.java),
            ActivityClass("", MultiWebViewPlaceholder4Activity::class.java),
            ActivityClass("", MultiWebViewPlaceholder5Activity::class.java),
        )
        val controllerMap = mutableMapOf<Mmid, MultiWebViewController>()
        /**获取当前的controller, 只能给nativeUI 使用，因为他们是和mwebview绑定在一起的*/
        fun getCurrentWebViewController(mmid: Mmid): MultiWebViewController? {
            return controllerMap[mmid]
        }
    }

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        /// nativeui 与 mwebview 是伴生关系
        bootstrapContext.dns.bootstrap("nativeui.sys.dweb")

        // 打开webview
        val queryUrl = Query.string().required("url")
        val queryWebviewId = Query.string().required("webview_id")

        apiRouting = routes(
            // 打开一个 webview 作为窗口
            "/open" bind Method.GET to defineHandler { request, ipc ->
                val url = queryUrl(request)
                val remoteMm = ipc.asRemoteInstance()
                    ?: throw Exception("mwebview.sys.dweb/open should be call by locale")
                val viewItem = openDwebView(remoteMm, url)
                Response(Status.OK).body(viewItem.webviewId)
            },
            // 关闭指定 webview 窗口
            "/close" bind Method.GET to defineHandler { request, ipc ->
                val webviewId = queryWebviewId(request)
                val remoteMmid = ipc.remote.mmid

                closeDwebView(remoteMmid, webviewId)
            },
            // 界面没有关闭，用于重新唤醒
            "/activate" bind Method.GET to defineHandler { request, ipc ->
                val remoteMmid = ipc.remote.mmid
                val webViewId = queryWebviewId(request)
                debugMultiWebView("REOPEN-WEBVIEW", "remote-mmid: $remoteMmid==>$webViewId")
                activityClassList.find { it.mmid == remoteMmid }?.let { activityClass ->
                    App.startActivity(activityClass.ctor) { intent ->
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                        val b = Bundle()
                        b.putString("mmid", remoteMmid)
                        intent.putExtras(b)
                    }
                }
                return@defineHandler Response(Status.OK).body(webViewId)
            },
        )
    }

    override suspend fun _shutdown() {
        apiRouting = null
    }


    override fun openActivity(remoteMmid: Mmid){
        val flags = mutableListOf<Int>(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        val activityClass = activityClassList.find { it.mmid == remoteMmid } ?:
        // 如果没有，从第一个挪出来，放到最后一个，并将至付给 remoteMmid
        activityClassList.removeAt(0).also {
            it.mmid = remoteMmid
            activityClassList.add(it)
            flags.add(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        App.startActivity(activityClass.ctor) { intent ->
            // intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
//            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val b = Bundle();
            b.putString("mmid", remoteMmid);
            intent.putExtras(b);
        }
    }


    private suspend fun openDwebView(
        remoteMm: MicroModule,
        url: String,
    ): MultiWebViewController.ViewItem {
        val remoteMmid = remoteMm.mmid
        debugMultiWebView("OPEN-WEBVIEW", "remote-mmid: $remoteMmid / url:$url")
        val controller = controllerMap.getOrPut(remoteMmid) {
            MultiWebViewController(
                remoteMmid,
                this,
                remoteMm,
            )
        }

        openActivity(remoteMmid)
        activitySignal.emit(Pair(remoteMmid, controller.waitActivityCreated()))
        return controller.openWebView(url)
    }

    private suspend fun closeDwebView(remoteMmid: String, webviewId: String): Boolean {
        onDestroySignal.emit(remoteMmid)
        return controllerMap[remoteMmid]?.closeWebView(webviewId) ?: false
    }

}
