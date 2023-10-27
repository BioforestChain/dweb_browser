package org.dweb_browser.browser.mwebview

import androidx.compose.ui.platform.LocalDensity
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import io.ktor.http.HttpMethod
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.helper.removeWhen
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.getWindow

suspend fun MultiWebViewNMM.webViewSysProtocol() {
  protocol("webview.sys.dweb") {

    routes(
      /// 提供句柄与链接，将链接进行渲染
      "/open" bind HttpMethod.Post to defineEmptyResponse {
        val rid = request.query("rid")
        val url = request.query("url")
        val wid = request.query("wid")
        val remoteMm = ipc.remoteAsInstance()
          ?: throw Exception("webview.sys.dweb/open should be call by locale")
        val win = remoteMm.getWindow(wid)
        val engine = win.createDwebView(remoteMm, url)

        windowAdapterManager.provideRender(rid) { modifier ->
          val webViewScale = (LocalDensity.current.density * scale * 100).toInt()
          engine.setInitialScale(webViewScale)
          WebView(state = rememberWebViewState(url = url), modifier = modifier, factory = {
            engine
          })
        }.removeWhen(ipc.onClose)

      }).cors()
  }
}