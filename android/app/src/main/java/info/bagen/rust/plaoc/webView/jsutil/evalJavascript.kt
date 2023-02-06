package info.bagen.rust.plaoc.webView.jsutil

import android.util.Log
import android.webkit.ValueCallback
import info.bagen.rust.plaoc.webView.dWebView

/** 传递字符串触发前端事件*/
fun returnStringForJs(cmd:String,data:String) {
    sendToJavaScript("nativeListen.dispatchStringMessage('$cmd','$data')")
}

/** 在webView运行js*/
fun sendToJavaScript(jsCode: String) {
    dWebView?.post {
        Log.e("evalJavascript", "sendToJavaScript jsCode=$jsCode")
        dWebView?.evaluateJavascript(jsCode, ValueCallback<String> { result ->
            if (result.isNotEmpty() && result != "null") {
                Log.e("evalJavascript", "sendToJavaScript 返回的数据=$result")
            }
        })
    }
}

/**
 * 触发后退事件
 */
fun emitListenBackButton() {
    Log.e("evalJavascript", "emitListenBackButton 触发了监听回调事件")
    emitJSListener("dweb-app", ListenFunction.ListenBackButton.value, "{canGoBack:true}")
}

/**
 * 触发js 事件监听
 */
fun emitJSListener(wb: String, func: String, data: Any) {
    sendToJavaScript("javascript:document.querySelector('${wb}').notifyListeners('${func}',${data})")
}

/**
 * 如果有监听事件，需要在此处添加
 */
enum class ListenFunction(val value: String) {
    ListenBackButton("ListenBackButton"), // 监听后退事件 （android only）
}
