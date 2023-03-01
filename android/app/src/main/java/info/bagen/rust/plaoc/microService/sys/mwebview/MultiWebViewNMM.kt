package info.bagen.rust.plaoc.microService.sys.mwebview

import android.content.Intent
import android.os.Bundle
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.microService.helper.printdebugln
import org.http4k.core.Method
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

inline fun debugMultiWebView(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("MultiWebViewNMM", tag, msg, err)


class MultiWebViewNMM : NativeMicroModule("mwebview.sys.dweb") {
    companion object {

        val activityMap = mutableMapOf<Mmid, PromiseOut<MutilWebViewActivity>>()
    }

    override suspend fun _bootstrap() {
        // 打开webview

        val query_url = Query.string().required("url")
        val query_webviewId = Query.string().required("webview_id")

        apiRouting = routes(
            "/open" bind Method.GET to defineHandler { request, ipc ->
                val url = query_url(request)
                openDwebView(ipc.remote, url)
            },
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
    private fun openMutilWebViewActivity(remoteMmid: Mmid) = activityMap.getOrPut(remoteMmid) {
        debugMultiWebView("OPEN-ACTIVITY", "remote-mmid: $remoteMmid")
        App.startActivity(MutilWebViewActivity::class.java) { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val b = Bundle();
            b.putString("mmid", remoteMmid);
            intent.putExtras(b);
        }
        PromiseOut()
    }

    private suspend fun openDwebView(
        remoteMm: MicroModule,
        url: String,
    ): String {
        val remoteMmid = remoteMm.mmid
        debugMultiWebView("OPEN-WEBVIEW", "remote-mmid: $remoteMmid / url:$url")
        val activity = openMutilWebViewActivity(remoteMmid).waitPromise()
        return activity.openWebView(remoteMm, url).webviewId
    }

    private suspend fun closeDwebView(remoteMmid: String, webviewId: String): Boolean {
        debugMultiWebView("OPEN-WEBVIEW", "remote-mmid: $remoteMmid / webview-id:$webviewId")
        val activity = activityMap[remoteMmid]?.waitPromise()
            ?: throw Exception("no found activity for mmid: $remoteMmid")

        return activity.closeWebView(webviewId)
    }
}
