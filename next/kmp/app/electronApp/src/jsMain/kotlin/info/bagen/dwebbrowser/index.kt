package info.bagen.dwebbrowser


import electron.app
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.MessageEvent
import org.w3c.dom.MessagePort
import kotlin.js.Date
import kotlin.js.Promise

fun startPureViewController(viewModalPort: MessagePort) {
  println("run startPureViewController: $viewModalPort")
  viewModalPort.addEventListener("message", {
    require(it is MessageEvent)
    println("startPureViewController/got message/data: ${it.data}")
  })
  viewModalPort.start()
  CoroutineScope(Dispatchers.Unconfined).launch {
    delay(1000)
    viewModalPort.postMessage(Date.now())
  }
}

suspend fun main() {
  println("js xxxxxxxx info.bagen.dwebbrowser")
  println("js yyyyyyyy info.bagen.dwebbrowser")
  println("js zzzzzzzz info.bagen.dwebbrowser")
  js.globals.globalThis.startPureViewController = ::startPureViewController
  console.log("startPureViewController", js.globals.globalThis.startPureViewController)
  (js("import('dweb-browser-kmp-wasmBackend-wasm-js');") as Promise<Unit>).await()

  app.whenReady().await()

  val win = createBrowserWindow {
    width = 600.0
    height = 200.0
  }
  win.loadURL("https://baidu.com")
}

fun <T> Promise<T>.toDeferred(): Deferred<T> {
  val deferred = CompletableDeferred<T>()
  then(onFulfilled = {
    deferred.complete(it)
  }, onRejected = {
    deferred.completeExceptionally(it)
  })
  return deferred
}

suspend fun <T> Promise<T>.await() = toDeferred().await()