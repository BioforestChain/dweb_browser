package org.dweb_browser.dwebview

import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import okio.buffer
import org.dweb_browser.core.http.dwebHttpGatewayServer
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.core.std.file.getApplicationRootDir
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.OffListener
import org.dweb_browser.helper.SimpleCallback
import org.dweb_browser.helper.SystemFileSystem
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.getKtorServerEngine
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource

val debugDWebView = Debugger("dwebview")

expect suspend  fun IDWebView.Companion.create(
  mm: MicroModule,
  options: DWebViewOptions = DWebViewOptions()
): IDWebView

interface IDWebView {
  @OptIn(ExperimentalResourceApi::class)
  companion object {
    private suspend fun findPort(): UShort {
      val server = embeddedServer(getKtorServerEngine(), port = 0) {};
      server.start(false)
      return server.resolvedConnectors().first().port.toUShort().also { server.stop() }
    }

    private val proxyAddress = CompletableDeferred<String>()
    suspend fun getProxyAddress() = proxyAddress.await()

    init {
      CoroutineScope(ioAsyncExceptionHandler).launch {
        println("reverse_proxy starting")
        val proxyServerPort = async { findPort() }
        val frontendServerPort = async { findPort() }
        val backendServerPort = async { dwebHttpGatewayServer.startServer().toUShort() }
        val dwebResourceDir = FileNMM.Companion.getApplicationRootDir().resolve("dwebview").also {
          if (!SystemFileSystem.exists(it)) {
            SystemFileSystem.createDirectories(it)
          }
        }
        val frontendCertsPath = dwebResourceDir.resolve("dweb.pem").also {
          if (!SystemFileSystem.exists(it)) {
            SystemFileSystem.sink(it).buffer().write(resource("dwebview/dweb.pem").readBytes())
              .close()
          }
        }
        val frontendKeyPath = dwebResourceDir.resolve("dweb.rsa").also {
          if (!SystemFileSystem.exists(it)) {
            SystemFileSystem.sink(it).buffer().write(resource("dwebview/dweb.rsa").readBytes())
              .close()
          }
        }
        println("reverse_proxy running proxyServerPort=${proxyServerPort.await()}, frontendServerPort=${frontendServerPort.await()}, backendServerPort=${backendServerPort.await()}")
        proxyAddress.complete("http://127.0.0.1:${proxyServerPort.await()}")
        reverse_proxy.start(
          proxyServerPort.await(),
          frontendServerPort.await(),
          frontendCertsPath.toString(),
          frontendKeyPath.toString(),
          backendServerPort.await(),
        )
        println("reverse_proxy stopped")
      }
    }
  }

  suspend fun loadUrl(url: String, force: Boolean = false): String
  suspend fun getUrl(): String
  suspend fun getTitle(): String
  suspend fun getIcon(): String
  suspend fun destroy()
  suspend fun canGoBack(): Boolean
  suspend fun canGoForward(): Boolean
  suspend fun goBack(): Boolean
  suspend fun goForward(): Boolean

  suspend fun createMessageChannel(): IWebMessageChannel
  suspend fun postMessage(data: String, ports: List<IWebMessagePort>)

  suspend fun setContentScale(scale: Float)

  suspend fun evaluateAsyncJavascriptCode(
    script: String,
    afterEval: suspend () -> Unit = {}
  ): String

  fun onDestroy(cb: SimpleCallback): OffListener<Unit>
  suspend fun onReady(cb: SimpleCallback)
}

internal class LoadUrlTask(
  val url: String,
  val deferred: CompletableDeferred<String> = CompletableDeferred()
)

typealias AsyncChannel = Channel<Result<String>>