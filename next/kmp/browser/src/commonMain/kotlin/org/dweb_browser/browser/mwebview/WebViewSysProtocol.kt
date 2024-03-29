package org.dweb_browser.browser.mwebview

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import org.dweb_browser.browser.common.createDwebView
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.dwebview.Render
import org.dweb_browser.helper.removeWhen
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.window.core.windowAdapterManager
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
          val density = LocalDensity.current.density
          LaunchedEffect(scale, width, height) {
            webView.setContentScale(scale, width, height, density)
          }
          webView.Render(modifier)
        }.removeWhen(ipc.scope)

      }).cors()
  }
}