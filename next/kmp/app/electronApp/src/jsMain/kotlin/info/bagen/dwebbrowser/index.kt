package info.bagen.dwebbrowser

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.dweb_browser.js_backend.browser_window.ElectronBrowserWindowModule
import kotlin.js.Promise

suspend fun main() {
  ElectronBrowserWindowModule("demo.compose.app", mutableMapOf<dynamic,dynamic>("currentCount" to 10))
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