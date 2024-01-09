package info.bagen.dwebbrowser


import electron.app
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlin.js.Promise

suspend fun main() {
  println("js xxxxxxxx info.bagen.dwebbrowser")
  println("js yyyyyyyy info.bagen.dwebbrowser")
  println("js zzzzzzzz info.bagen.dwebbrowser")
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