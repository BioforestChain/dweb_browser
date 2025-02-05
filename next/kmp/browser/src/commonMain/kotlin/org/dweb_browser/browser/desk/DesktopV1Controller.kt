package org.dweb_browser.browser.desk

import androidx.compose.runtime.Composable
import dweb_browser_kmp.browser.generated.resources.Res
import io.ktor.http.Url
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.filter
import org.dweb_browser.browser.common.createDwebView
import org.dweb_browser.browser.desk.render.RenderImpl
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.helper.IpcServerRequest
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.ext.ResponseLocalFileBase
import org.dweb_browser.core.std.http.CORS_HEADERS
import org.dweb_browser.core.std.http.DwebHttpServerOptions
import org.dweb_browser.core.std.http.HttpDwebServer
import org.dweb_browser.core.std.http.createHttpDwebServer
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.helper.Producer
import org.dweb_browser.helper.build
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.resolvePath
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.queryAsOrNull
import org.jetbrains.compose.resources.ExperimentalResourceApi

class DesktopV1Controller private constructor(
  viewController: IPureViewController,
  deskNMM: DeskNMM.DeskRuntime,
  private val desktopServer: HttpDwebServer,
) : DesktopControllerBase(viewController, deskNMM) {

  companion object {

    private val API_PREFIX = "/api/"
    internal suspend fun commonWebServerFactory(
      subdomain: String,
      deskNMM: DeskNMM.DeskRuntime,
    ): HttpDwebServer {
      val desktopServer = deskNMM.createHttpDwebServer(DwebHttpServerOptions(subdomain))
      val serverIpc = desktopServer.listen()
      serverIpc.onRequest("WebServer/$subdomain")
        .collectIn(deskNMM.getRuntimeScope(), commonWebServerHandler(deskNMM, serverIpc))
      return desktopServer
    }

    @OptIn(ExperimentalResourceApi::class)
    internal fun commonWebServerHandler(deskNMM: DeskNMM.DeskRuntime, serverIpc: Ipc) =
      FlowCollector<Producer<IpcServerRequest>.Event> { event ->
        val ipcServerRequest = event.consume()
        val pathName = ipcServerRequest.uri.encodedPathAndQuery
        val pureResponse = if (pathName.startsWith(API_PREFIX)) {
          val apiUri = "file://${pathName.substring(API_PREFIX.length)}"
          val response =
            deskNMM.nativeFetch(ipcServerRequest.toPure().toClient().copy(href = apiUri))
          PureResponse.build(response) { appendHeaders(CORS_HEADERS) }
        } else {
          val filePath = ipcServerRequest.uri.encodedPath
          val resBinary = Res.readBytes("files/browser-desk${filePath}")
          ResponseLocalFileBase(filePath, false).returnFile(resBinary)
        }
        serverIpc.postResponse(ipcServerRequest.reqId, pureResponse)
      }

    private suspend fun configRoutes(
      desktopController: DesktopV1Controller,
      deskNMM: DeskNMM.DeskRuntime,
    ) {
      DesktopControllerBase.configSharedRoutes(desktopController, deskNMM)
      with(deskNMM) {
        val mmScope = deskNMM.getRuntimeScope()
        routes(
          // 获取全部app数据
          "/desktop/apps" bind PureMethod.GET by defineJsonResponse {
            debugDesk("/desktop/apps", desktopController.getDesktopApps())
            return@defineJsonResponse desktopController.getDesktopApps().toJsonElement()
          },
          // 监听所有app数据
          "/desktop/observe/apps" byChannel { ctx ->
            // 默认不同步 bounds 字段，否则move的时候数据量会非常大
            val enableBounds = request.queryAsOrNull<Boolean>("bounds") ?: false
            val job = desktopController.onUpdate.run {
              when {
                enableBounds -> this
                // 如果只有 bounds ，那么忽略，不发送
                else -> filter { it != "bounds" }
              }
            }.collectIn(mmScope) {
              debugDesk("/desktop/observe/apps") { "changes=$it" }
              try {
                val apps = desktopController.getDesktopApps()
                ctx.sendJsonLine(apps)
              } catch (e: Throwable) {
                close(cause = e)
              }
            }
            onClose {
              job.cancel()
            }
            desktopController.updateFlow.emit("init")
          },
        )
      }
    }

    suspend fun create(
      deskNMM: DeskNMM.DeskRuntime,
      viewController: IPureViewController,
    ): DesktopV1Controller {
      val desktopServer = commonWebServerFactory("desktop", deskNMM)
      val controller = DesktopV1Controller(viewController, deskNMM, desktopServer)
      controller.desktopView = controller.createDesktopView()
      configRoutes(controller, deskNMM)
      return controller
    }
  }

  internal lateinit var desktopView: IDWebView
  private suspend fun createDesktopView(): IDWebView {
    val options = DWebViewOptions(
      url = getDesktopUrl().toString(),
      privateNet = true,
      openDevTools = envSwitch.isEnabled(ENV_SWITCH_KEY.DESKTOP_DEVTOOLS),
      detachedStrategy = DWebViewOptions.DetachedStrategy.Ignore,
      displayCutoutStrategy = DWebViewOptions.DisplayCutoutStrategy.Default,
      viewId = 1,
    )

    val webView = viewController.createDwebView(deskNMM, options)
    // 隐藏滚动条
    webView.setVerticalScrollBarVisible(false)
    webView.setHorizontalScrollBarVisible(false)

    deskNMM.onBeforeShutdown {
      deskNMM.scopeLaunch(cancelable = false) {
        webView.destroy()
      }
    }
    return webView
  }

  @Composable
  override fun Render() {
    RenderImpl()
  }

  private fun getDesktopUrl() = when (val url = envSwitch.get(ENV_SWITCH_KEY.DESKTOP_DEV_URL)) {
    "" -> desktopServer.startResult.urlInfo.buildInternalUrl().build {
      resolvePath("/desktop.html")
    }

    else -> Url(url)
  }
}
