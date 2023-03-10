package info.bagen.rust.plaoc.microService.sys.mwebview

import android.content.Intent
import android.os.Bundle
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.ioAsyncExceptionHandler
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.IpcEvent
import io.ktor.util.collections.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.http4k.core.Method
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.util.concurrent.ConcurrentSkipListSet

inline fun debugMultiWebView(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("mwebview", tag, msg, err)


class MultiWebViewNMM : NativeMicroModule("mwebview.sys.dweb") {
    class ActivityClass(var mmid: Mmid, val ctor: Class<out MutilWebViewActivity>)


    companion object {
        val activityClassList = mutableListOf(
            ActivityClass("", MutilWebViewPlaceholder1Activity::class.java),
            ActivityClass("", MutilWebViewPlaceholder2Activity::class.java),
            ActivityClass("", MutilWebViewPlaceholder3Activity::class.java),
            ActivityClass("", MutilWebViewPlaceholder4Activity::class.java),
            ActivityClass("", MutilWebViewPlaceholder5Activity::class.java),
        )
        val controllerMap = mutableMapOf<Mmid, MutilWebViewController>()
        fun getCurrentWebViewController(mmid: Mmid): MutilWebViewController? {
            return controllerMap[mmid]
        }
    }

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        // 打开webview

        val query_url = Query.string().required("url")
        val query_webviewId = Query.string().required("webview_id")

        val subscribers = ConcurrentMap<Ipc, ConcurrentSkipListSet<String>>()
        val job = GlobalScope.launch(ioAsyncExceptionHandler) {
            while (true) {
                for ((ipc) in subscribers) {
                    ipc.postMessage(IpcEvent.fromUtf8("qaq", "hi"))
                }
                delay(1000)
            }
        }
        _afterShutdownSignal.listen { job.cancel() }

        apiRouting = routes(
            // 打开一个 webview 作为窗口
            "/open" bind Method.GET to defineHandler { request, ipc ->
                val url = query_url(request)
                val remoteMm = ipc.asRemoteInstance()
                    ?: throw Exception("mwebview.sys.dweb/open should be call by locale")

                val webviewId = openDwebView(remoteMm, url)
                val refs = subscribers.getOrPut(ipc) { ConcurrentSkipListSet<String>() }
                refs.add(webviewId)

                webviewId
            },
            // 关闭指定 webview 窗口
            "/close" bind Method.GET to defineHandler { request, ipc ->
                val webviewId = query_webviewId(request)
                val remoteMmid = ipc.remote.mmid

                closeDwebView(remoteMmid, webviewId)
            })
    }

    override suspend fun _shutdown() {
        apiRouting = null
    }

    @Synchronized
    private fun openMutilWebViewActivity(remoteMmid: Mmid): ActivityClass {
        val flags = mutableListOf<Int>(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        val activityClass = activityClassList.find { it.mmid == remoteMmid } ?:
        // 如果没有，从第一个挪出来，放到最后一个，并将至付给 remoteMmid
        activityClassList.removeAt(0).also {
            it.mmid = remoteMmid
            activityClassList.add(it)
            flags.add(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        App.startActivity(activityClass.ctor) { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
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
    ): String {
        val remoteMmid = remoteMm.mmid
        debugMultiWebView("OPEN-WEBVIEW", "remote-mmid: $remoteMmid / url:$url")
        val controller = controllerMap.getOrPut(remoteMmid) {
            MutilWebViewController(
                remoteMmid,
                this,
                remoteMm,
            )
        }
        openMutilWebViewActivity(remoteMmid)
        controller.waitActivityCreated()
        return controller.openWebView(url).webviewId
    }

    private fun closeDwebView(remoteMmid: String, webviewId: String) =
        controllerMap[remoteMmid]?.let {
            it.closeWebView(webviewId)
        } ?: false
}
