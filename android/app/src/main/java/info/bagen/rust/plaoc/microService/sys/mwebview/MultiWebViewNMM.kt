package info.bagen.rust.plaoc.microService.sys.mwebview

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.mutableStateMapOf
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.ipc.IpcEvent
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.ipc.Ipc
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

inline fun debugMultiWebView(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("mwebview", tag, msg, err)

class MultiWebViewNMM : NativeMicroModule("mwebview.sys.dweb") {
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

        /**获取当前的controller*/
        fun getCurrentWebViewController(mmid: Mmid): MultiWebViewController? {
            return controllerMap[mmid]
        }
    }

    private val mIpcMap = mutableStateMapOf<Ipc, MutableMap<String, MultiWebViewController.ViewItem>>()

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        /// nativeui 与 mwebview 是伴生关系
        bootstrapContext.dns.bootstrap("nativeui.sys.dweb")

        // 打开webview
        val queryUrl = Query.string().required("url")
        val queryWebviewId = Query.string().required("webview_id")

//        val subscribers = ConcurrentMap<Ipc, ConcurrentSkipListSet<String>>()
//        val job = GlobalScope.launch(ioAsyncExceptionHandler) {
//            while (true) {
//                for ((ipc) in subscribers) {
//                    ipc.postMessage(IpcEvent.fromUtf8("close", "hi"))
//                }
//                delay(1000)
//            }
//        }
//        _afterShutdownSignal.listen { job.cancel() }

        apiRouting = routes(
            // 打开一个 webview 作为窗口
            "/open" bind Method.GET to defineHandler { request, ipc ->
                val url = queryUrl(request)
                val remoteMm = ipc.asRemoteInstance()
                    ?: throw Exception("mwebview.sys.dweb/open should be call by locale")

                val viewItem = openDwebView(remoteMm, url)
//                subscribers.getOrPut(ipc) { ConcurrentSkipListSet<String>() }.also { refs ->
//                    refs.add(viewItem.webviewId)
//                }
                mIpcMap.getOrPut(ipc) {
                    ipc.onEvent {
                        debugMultiWebView("event", "name=${it.event.name},data=${it.event.data}")
                    }
                    mutableMapOf()
                }.apply { this[viewItem.webviewId] = viewItem }

                //Response(Status.OK).body(viewItem.webviewId)
                viewItem.webviewId
            },
            // 关闭指定 webview 窗口
            "/close" bind Method.GET to defineHandler { request, ipc ->
                val webviewId = queryWebviewId(request)
                val remoteMmid = ipc.remote.mmid

                closeDwebView(remoteMmid, webviewId)
            },
            // 界面没有关闭，用于重新唤醒
            "/reOpen" bind Method.GET to defineHandler { request, ipc ->
                val remoteMmid = ipc.remote.mmid
                val webViewId = queryWebviewId(request)
                debugMultiWebView("REOPEN-WEBVIEW", "remote-mmid: $remoteMmid==>$webViewId")
                activityClassList.find { it.mmid == remoteMmid }?.let { activityClass ->
                    App.startActivity(activityClass.ctor) { intent ->
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                        val b = Bundle();
                        b.putString("mmid", remoteMmid);
                        intent.putExtras(b);
                    }
                }
                return@defineHandler Response(Status.OK).body(webViewId)
            },
        )
    }

    override suspend fun _shutdown() {
        apiRouting = null
    }

    @Synchronized
    private fun openMultiWebViewActivity(remoteMmid: Mmid): ActivityClass {
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
        return activityClass
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
        openMultiWebViewActivity(remoteMmid)
        controller.waitActivityCreated()
        return controller.openWebView(url)
    }

    suspend fun closeDwebView(remoteMmid: String, webviewId: String) {
        for ((ipc, value) in mIpcMap) {
            if (ipc.remote.mmid == remoteMmid) {
                ipc.postMessage(IpcEvent.fromUtf8("state", "xxxxxx"))
                break
            }
        }
        controllerMap[remoteMmid]?.closeWebView(webviewId) ?: false
    }
}
