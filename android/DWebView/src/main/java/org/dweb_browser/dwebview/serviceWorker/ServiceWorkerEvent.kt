package org.dweb_browser.dwebview.serviceWorker

import org.dweb_browser.helper.*
import kotlinx.coroutines.withContext
import org.dweb_browser.dwebview.DWebView

const val DWEB_SERVICE_WORKER = "__app_upgrade_watcher_kit__"

enum class DownloadControllerEvent(val event: String) {
  Start("start"), // 监听启动
  Progress("progress"), // 进度每秒触发一次
  End("end"), // 结束
  Cancel("cancel"), // 取消
  Pause("pause"), // 暂停
}

suspend fun emitEvent(dwebView: DWebView, eventName: String, data: String = ""): Boolean {
  var payload = """new Event("$eventName")"""
  // progress,fetch,onFetch为自定义构造返回
  if (eventName == DownloadControllerEvent.Progress.event) {
    payload = data
  }
  /// 尝试去触发客户端的监听，如果客户端有监听的话
  withContext(mainAsyncExceptionHandler) {
    dwebView.evaluateAsyncJavascriptCode(
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