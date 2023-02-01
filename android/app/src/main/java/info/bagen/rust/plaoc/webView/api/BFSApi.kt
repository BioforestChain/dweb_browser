package info.bagen.rust.plaoc.webView.api

import android.content.Intent
import android.util.Log
import android.webkit.JavascriptInterface
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.broadcast.BFSBroadcastAction

class BFSApi {

    @JavascriptInterface
    fun BFSInstallApp(path: String) {
        Log.d("BFSApi", "BFSInstallApp path=$path")
        val intent = Intent(BFSBroadcastAction.BFSInstallApp.action).apply {
            putExtra("path", path)
        }
        App.appContext.sendBroadcast(intent)
    }

    @JavascriptInterface
    fun getConnectChannel(url: String): String {
        Log.d("BFSApi", "getConnectChannel url=$url")
        return url
    }

    @JavascriptInterface
    fun postConnectChannel(url: String, cmd: String, buf: String): String  {
        Log.d("BFSApi", "postConnectChannel cmd=$cmd,url=$url,buf=$buf")
        return url
    }
}
