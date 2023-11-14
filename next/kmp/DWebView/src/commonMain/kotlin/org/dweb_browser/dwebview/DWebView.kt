package org.dweb_browser.dwebview

import io.ktor.server.engine.embeddedServer
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import okio.buffer
import org.dweb_browser.core.http.dwebHttpGatewayServer
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.core.std.file.getApplicationRootDir
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SystemFileSystem
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.getKtorServerEngine
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource

val debugDWebView = Debugger("dwebview")

expect suspend fun IDWebView.Companion.create(
  mm: MicroModule,
  options: DWebViewOptions = DWebViewOptions()
): IDWebView

abstract class IDWebView(initUrl: String?) {
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


  protected abstract suspend fun startLoadUrl(url: String)

  private val loadUrlTask = atomic(if (initUrl.isNullOrEmpty()) null else LoadUrlTask(initUrl))

  suspend fun loadUrl(url: String, force: Boolean = false) = loadUrlTask.getAndUpdate { preTask ->
    if (!force && preTask != null && preTask.url == url) {
      return@getAndUpdate preTask
    } else {
      preTask?.deferred?.cancel(CancellationException("load new url: $url"));
    }
    val newTask = LoadUrlTask(url)
    loadUrl(newTask)
    newTask.deferred.invokeOnCompletion {
      loadUrlTask.getAndUpdate { preTask ->
        if (preTask == newTask) null else preTask
      }
    }
    newTask
  }!!.deferred.await()

  private suspend fun loadUrl(task: LoadUrlTask): String {
    startLoadUrl(task.url)

    val off = onLoadStateChange {
      when (it) {
        is WebLoadErrorState -> {
          task.deferred.completeExceptionally(Exception(it.errorMessage))
        }

        is WebLoadStartState -> {
          if (it.url != task.url) {
            task.deferred.cancel(CancellationException("start load url: ${it.url}"))
          }
        }

        is WebLoadSuccessState -> {
          task.deferred.complete(it.url)
        }
      }
    }
    try {
      return task.deferred.await()
    } finally {
      off()
    }
  }

  fun getUrl() = loadUrlTask.value?.url ?: "about:blank"
  fun hasUrl() = loadUrlTask.value?.url.isNullOrBlank()
  abstract suspend fun getTitle(): String
  abstract suspend fun getIcon(): String
  abstract suspend fun destroy()
  abstract suspend fun canGoBack(): Boolean
  abstract suspend fun canGoForward(): Boolean
  abstract suspend fun goBack(): Boolean
  abstract suspend fun goForward(): Boolean

  abstract suspend fun createMessageChannel(): IWebMessageChannel
  abstract suspend fun postMessage(data: String, ports: List<IWebMessagePort>)

  abstract suspend fun setContentScale(scale: Float)
  abstract suspend fun setPrefersColorScheme(colorScheme: WebColorScheme)

  abstract suspend fun evaluateAsyncJavascriptCode(
    script: String,
    afterEval: suspend () -> Unit = {}
  ): String

  abstract val onDestroy: Signal.Listener<Unit>
  abstract val onLoadStateChange: Signal.Listener<WebLoadState>
  abstract val onReady: Signal.Listener<String>
  abstract val onBeforeUnload: Signal.Listener<WebBeforeUnloadArgs>
  abstract val loadingProgressFlow: SharedFlow<Float>
  abstract val closeWatcher: ICloseWatcher
  abstract val onCreateWindow: Signal.Listener<IDWebView>
}

class WebBeforeUnloadArgs(
  val message: String,
) : SynchronizedObject() {
  private val hookReasons = mutableMapOf<Any, WebBeforeUnloadHook>()
  internal fun hook(reason: Any) =
    synchronized(this) {
      if (isLocked) throw Exception("fail to hook, event already passed.")
      hookReasons.getOrPut(reason) { WebBeforeUnloadHook(message) }
    }

  private var isLocked = false
  private fun syncLock() = synchronized(this) { isLocked = true }

  private val result = CompletableDeferred<Boolean>()
  internal suspend fun waitHookResults() = syncLock().let {
    var isKeep = false;
    val results = hookReasons.values.map { it.result.await() }
    for (unload in results) {
      if (!unload) {
        isKeep = true;
        break
      }
    }
    val unload = !isKeep
    result.complete(unload)
    unload
  }


  suspend fun isKeep() = !result.await()
  suspend fun isUnload() = result.await()
}

class WebBeforeUnloadHook(val message: String) {
  internal val result = CompletableDeferred<Boolean>()
  fun unloadDocument() = result.complete(true)
  fun keepDocument() = result.complete(false)
}


sealed class WebLoadState {};
class WebLoadStartState(val url: String) : WebLoadState();
class WebLoadSuccessState(val url: String) : WebLoadState();
class WebLoadErrorState(val url: String, val errorMessage: String) : WebLoadState();

fun Signal<WebLoadState>.toReadyListener() =
  createChild({ if (it is WebLoadSuccessState) it else null },
    { it.url }).toListener()

internal data class LoadUrlTask(val url: String) {
  val deferred = CompletableDeferred<String>()
}


enum class WebColorScheme {
  Normal,
  Dark,
  Light,
}

typealias AsyncChannel = Channel<Result<String>>