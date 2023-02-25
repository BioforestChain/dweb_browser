package info.bagen.rust.plaoc.webView.api

import android.content.Intent
import android.util.Log
import android.webkit.JavascriptInterface
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.broadcast.BFSBroadcastAction

class BFSApi {

    @JavascriptInterface
    fun BFSInstallApp(path: String) {
        val intent = Intent(BFSBroadcastAction.BFSInstallApp.action).apply {
            putExtra("path", path)
        }
        App.appContext.sendBroadcast(intent)
    }

    @JavascriptInterface
    fun BFSGetConnectChannel(url: String): String {
        filtrationHandle(url)
        return url
    }

    @JavascriptInterface
    fun BFSPostConnectChannel(url: String, cmd: String, buf: String): String  {
        if (url.isNotEmpty()) return "request data undefined"
        return url
    }
    private fun filtrationHandle(url: String) {
        if (url.startsWith("file://")){
            return getMicroHandle(url)
        }
        getNativeHandle(url)
    }
    /**处理setUi poll */
    private fun getNativeHandle(url: String) {
        if (url.startsWith("poll") || url.startsWith("setUi")) {
//            return jsGateWay(customUrlScheme, request)
        }
    }
    /**处理file:// */
    private fun getMicroHandle(url: String) {
//        global_micro_dns.nativeFetch(url)
    }
}
