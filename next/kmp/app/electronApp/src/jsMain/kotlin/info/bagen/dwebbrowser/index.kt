package info.bagen.dwebbrowser

import electron.app
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlin.js.Promise

suspend fun main() {

  BrowserViewModel("js.backend.dweb")
  val electronAppHttpSever = ElectronAppHttpServer().whenReady.await()
  electronAppHttpSever.start(8888)

  app.whenReady().await()

  val win = createBrowserWindow {
    width = 1000.0
    height = 600.0
  }

  win.loadURL("${electronAppHttpSever.getAddress()}/jsFrontEnd/index.html")
  win.webContents.openDevTools()
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