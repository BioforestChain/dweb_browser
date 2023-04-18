package info.bagen.dwebbrowser.microService.sys.nativeui.dwebServiceWorker

import info.bagen.dwebbrowser.microService.helper.Mmid
import info.bagen.dwebbrowser.microService.helper.mainAsyncExceptionHandler
import info.bagen.dwebbrowser.microService.sys.mwebview.MultiWebViewNMM
import kotlinx.coroutines.withContext


const val DWEB_SERVICE_WORKER = "__app_upgrade_watcher_kit__"


enum class ServiceWorkerEvent(val event: String) {
    UpdateFound("updatefound"), // 更新或重启的时候触发
    Fetch("fetch"), OnFetch("onFetch"), Start("start"), // 监听启动
    Progress("progress"), // 进度每秒触发一次
    End("end"), // 结束
    Cancel("cancel"), // 取消
}

suspend fun emitEvent(mmid: Mmid, eventName: String): Boolean {
    val controller = MultiWebViewNMM.getCurrentWebViewController(mmid) ?: return false
    /// 尝试去触发客户端的监听，如果客户端有监听的话
    withContext(mainAsyncExceptionHandler) {
        controller.lastViewOrNull?.webView?.evaluateAsyncJavascriptCode(
            """
            new Promise((resolve,reject)=>{
                try{
                    const listeners = ${DWEB_SERVICE_WORKER}._listeners["$eventName"];
                    if (listeners.length !== 0) {
                      listeners.forEach(listener => listener(new Event("$eventName")));
                      resolve(true)
                    }
                    resolve(false)
                }catch(err){console.log(err);resolve(false)}
            })
         
        """.trimIndent()
        ) {}
    }
    return true
}