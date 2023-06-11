package info.bagen.dwebbrowser.microService.browser.mwebview.dwebServiceWorker

import info.bagen.dwebbrowser.microService.helper.Mmid
import org.dweb_browser.helper.*
import info.bagen.dwebbrowser.microService.browser.mwebview.MultiWebViewNMM
import kotlinx.coroutines.withContext


const val DWEB_SERVICE_WORKER = "__app_upgrade_watcher_kit__"


enum class ServiceWorkerEvent(val event: String) {
    UpdateFound("updatefound"), // 更新或重启的时候触发
    Fetch("fetch"),
    OnFetch("onFetch"),
    Pause("pause"), // 暂停
    Resume("resume"),
}

enum class DownloadControllerEvent(val event:String) {
    Start("start"), // 监听启动
    Progress("progress"), // 进度每秒触发一次
    End("end"), // 结束
    Cancel("cancel"), // 取消
    Pause("pause"), // 暂停
}

suspend fun emitEvent(mmid: Mmid, eventName: String, data: String = ""): Boolean {
    val controller = MultiWebViewNMM.getCurrentWebViewController(mmid) ?: return false
    var payload = """new Event("$eventName")"""
    // progress,fetch,onFetch为自定义构造返回
    if (eventName == DownloadControllerEvent.Progress.event) {
        payload = data
    }
    /// 尝试去触发客户端的监听，如果客户端有监听的话
    withContext(mainAsyncExceptionHandler) {
        controller.lastViewOrNull?.webView?.evaluateAsyncJavascriptCode(
            """
            new Promise((resolve,reject)=>{
                try{
                    const listeners = $DWEB_SERVICE_WORKER._listeners["$eventName"];
                    if (listeners.length !== 0) {
                      listeners.forEach(listener => listener($payload));
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

//        ServiceWorkerEvent.Fetch.event-> {
//          payload = """
//              new FetchEvent("fetch",{
//              })
//          """.trimIndent()
//        }
//        ServiceWorkerEvent.OnFetch.event -> {
//            payload = """
//              new OnFetchEvent("fetch",{
//                response:Response(),
//                serverId:0
//              })
//          """.trimIndent()
//        }