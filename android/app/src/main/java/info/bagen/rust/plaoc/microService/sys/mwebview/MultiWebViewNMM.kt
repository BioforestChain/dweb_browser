package info.bagen.rust.plaoc.microService.sys.mwebview

import android.content.Intent
import android.os.Bundle
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.printdebugln
import org.http4k.core.Method
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

inline fun debugMultiWebView(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("MultiWebViewNMM", tag, msg, err)


class MultiWebViewNMM : NativeMicroModule("mwebview.sys.dweb") {
    data class ActivityClass(var mmid: Mmid, val ctor: Class<out MutilWebViewActivity>)
    companion object {
        val activityClassList = mutableListOf(
            ActivityClass("", MutilWebViewPlaceholder1Activity::class.java),
            ActivityClass("", MutilWebViewPlaceholder2Activity::class.java),
            ActivityClass("", MutilWebViewPlaceholder3Activity::class.java),
            ActivityClass("", MutilWebViewPlaceholder4Activity::class.java),
            ActivityClass("", MutilWebViewPlaceholder5Activity::class.java),
        )
        val controllerMap = mutableMapOf<Mmid, MutilWebViewController>()
    }

    override suspend fun _bootstrap() {
        // 打开webview

        val query_url = Query.string().required("url")
        val query_webviewId = Query.string().required("webview_id")

        apiRouting = routes(
            "/open" bind Method.GET to defineHandler { request, ipc ->
                val url = query_url(request)
                println("MultiWebViewNMM $url")
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
    private fun openMutilWebViewActivity(remoteMmid: Mmid) {
        val activityClass =
            activityClassList.find { it.mmid == remoteMmid } ?:
            // 如果没有，从第一个挪出来，放到最后一个，并将至付给 remoteMmid
            activityClassList.removeAt(0).also {
                it.mmid = remoteMmid
                activityClassList.add(it)
            }
        App.startActivity(activityClass.ctor) { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val b = Bundle();
            b.putString("mmid", remoteMmid);
            intent.putExtras(b);
        }
    }

    private fun openDwebView(
        remoteMm: MicroModule,
        url: String,
    ): String {
        val remoteMmid = remoteMm.mmid
        debugMultiWebView("OPEN-WEBVIEW", "remote-mmid: $remoteMmid / url:$url")
        val controller = controllerMap.getOrPut(remoteMmid) { MutilWebViewController(remoteMmid) }
        openMutilWebViewActivity(remoteMmid)
        return controller.openWebView(remoteMm, url).webviewId
    }

    private fun closeDwebView(remoteMmid: String, webviewId: String) =
        controllerMap[remoteMmid]?.let {
            it.closeWebView(webviewId)
        } ?: false
}
