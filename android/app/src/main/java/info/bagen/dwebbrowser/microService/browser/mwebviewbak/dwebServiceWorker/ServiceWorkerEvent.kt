package info.bagen.dwebbrowser.microService.browser.mwebviewbak.dwebServiceWorker

import org.dweb_browser.microservice.help.MMID
import info.bagen.dwebbrowser.microService.browser.mwebviewbak.MultiWebViewNMM

enum class ServiceWorkerEvent(val event: String) {
    UpdateFound("updatefound"), // 更新或重启的时候触发
    Fetch("fetch"),
    OnFetch("onFetch"),
    Pause("pause"), // 暂停
    Resume("resume"),
}

suspend fun emitEvent(mmid: MMID, eventName: String, data: String = ""): Boolean {
    val viewItem = MultiWebViewNMM.getCurrentWebViewController(mmid)?.lastViewOrNull ?: return false
    return org.dweb_browser.dwebview.serviceWorker.emitEvent(viewItem.webView, eventName, data)
}