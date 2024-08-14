package org.dweb_browser.browser.mwebview

import org.dweb_browser.browser.common.WindowControllerBinding
import org.dweb_browser.browser.common.createDwebView
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.dwebview.RenderWithScale
import org.dweb_browser.helper.removeWhen
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.core.withRenderScope
import org.dweb_browser.sys.window.ext.getWindow

suspend fun MultiWebViewNMM.MultiWebViewRuntime.webViewSysProtocol() {
  protocol("webview.sys.dweb") {

    routes(
      /// 提供句柄与链接，将链接进行渲染
      "/open" bind PureMethod.POST by defineEmptyResponse {
        val rid = request.query("rid")
        val url = request.query("url")
        val wid = request.query("wid")
        val remoteMm = getRemoteRuntime()
        val win = remoteMm.getWindow(wid)
        val webView = win.createDwebView(remoteMm, url)

        windowAdapterManager.provideRender(rid) { modifier ->
          webView.WindowControllerBinding()
          webView.RenderWithScale(scale, modifier.withRenderScope(this))
        }.removeWhen(win.lifecycleScope)
      }).cors()
  }
}