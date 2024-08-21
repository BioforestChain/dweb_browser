package org.dweb_browser.browser.about

import org.dweb_browser.browser.resources.Res
import io.ktor.http.HttpStatusCode
import io.ktor.http.path
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.file.ext.ResponseLocalFileBase
import org.dweb_browser.core.std.http.DwebHttpServerOptions
import org.dweb_browser.core.std.http.createHttpDwebServer
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.onRenderer
import org.jetbrains.compose.resources.ExperimentalResourceApi

class AboutNMM : NativeMicroModule("about.browser.dweb", "About") {
  init {
    name = AboutI18nResource.shortName.text
    short_name = AboutI18nResource.shortName.text
    categories = listOf(
      MICRO_MODULE_CATEGORY.Application
    )
    icons = listOf(
      ImageResource(src = "file:///sys/browser-icons/$mmid.svg", type = "image/svg+xml")
    )

    val brandMap = ENV_SWITCH_KEY.entries.mapNotNull { it.experimental }.associate { brandData ->
      brandData.brand to brandData.disableVersion
    }.toMutableMap()
    ENV_SWITCH_KEY.entries.forEach { switchKey ->
      val brandData = switchKey.experimental ?: return@forEach
      if (envSwitch.isEnabled(switchKey)) {
        brandMap[brandData.brand] = brandData.enableVersion
      }
    }
    brandMap.forEach { (brand, version) ->
      IDWebView.Companion.brands.add(IDWebView.UserAgentBrandData(brand, version))
    }
  }

  inner class AboutRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    override suspend fun _bootstrap() {
      startHtml5TestServer()
      onRenderer {
        getMainWindow().apply {
          setStateFromManifest(manifest)
          openAboutPage(id)
        }
      }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun startHtml5TestServer() {
      val html5testServer = createHttpDwebServer(DwebHttpServerOptions(subdomain = "html5test"))
      val serverIpc = html5testServer.listen()
      serverIpc.onRequest("html5test server").collectIn(mmScope) { event ->
        val request = event.consume()
        val filePath = request.uri.encodedPath
        val response = runCatching {
          val resBinary = Res.readBytes("files/browser-html5test${filePath}")
          ResponseLocalFileBase(filePath, false).returnFile(resBinary)
        }.getOrNull()
        serverIpc.postResponse(request.reqId, response ?: PureResponse(HttpStatusCode.NotFound))
      }
      onShutdown { scopeLaunch(cancelable = true) { html5testServer.close() } }

      val webview = IDWebView.create(this,
        DWebViewOptions(url = html5testServer.startResult.urlInfo.buildInternalUrl { path("index.html") }
          .toString()))

      _html5testWebView.complete(webview)
      webview.onReady.first()
      scopeLaunch(cancelable = true) {
        val scoreMessageChannel = webview.createMessageChannel()
        webview.postMessage("score-channel", listOf(scoreMessageChannel.port2))
        for (event in scoreMessageChannel.port1.onMessage) {
          _html5testScore.value = event.text
        }
      }
    }

    private val _html5testWebView = CompletableDeferred<IDWebView>()
    val html5testWebView get() = _html5testWebView as Deferred<IDWebView>

    private val _html5testScore = MutableStateFlow<String?>(null)
    val html5testScore = _html5testScore as StateFlow<String?>

    override suspend fun _shutdown() {}
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = AboutRuntime(bootstrapContext)
}

expect suspend fun AboutNMM.AboutRuntime.openAboutPage(id: UUID)

